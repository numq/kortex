package io.github.numq.kortex.core.cache

import io.github.numq.kortex.core.lifecycle.CloseableResource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

abstract class LruCache<Key, Value>(val capacity: Int) : CloseableResource(), Cache<Key, Value> {
    init {
        require(capacity > 0) { "Capacity must be greater than 0" }
    }

    abstract val factory: Key.() -> Value

    private class Node<Key, Value>(
        val key: Key,
        var value: Value,
        var prev: Node<Key, Value>? = null,
        var next: Node<Key, Value>? = null,
    )

    private val lock = SynchronizedObject()

    private val map = HashMap<Key, Node<Key, Value>>()

    private var head: Node<Key, Value>? = null

    private var tail: Node<Key, Value>? = null

    private fun closeResource(value: Value?) {
        if (value is AutoCloseable) {
            runCatching { value.close() }
        }
    }

    private fun detachNode(node: Node<Key, Value>) {
        val prev = node.prev

        val next = node.next

        when (prev) {
            null -> head = next

            else -> prev.next = next
        }

        when (next) {
            null -> tail = prev

            else -> next.prev = prev
        }

        node.prev = null

        node.next = null
    }

    private fun appendToTail(node: Node<Key, Value>) {
        node.prev = tail

        node.next = null

        when (val currentTail = tail) {
            null -> head = node

            else -> currentTail.next = node
        }

        tail = node
    }

    private fun moveToTail(node: Node<Key, Value>) {
        if (node === tail) return

        detachNode(node)

        appendToTail(node)
    }

    private fun clearInternal(): List<Value> = synchronized(lock) {
        val values = ArrayList<Value>(map.size)

        var current = head

        while (current != null) {
            values.add(current.value)

            current = current.next
        }

        head = null

        tail = null

        map.clear()

        values
    }

    override fun getOrCreate(key: Key): Result<Value> = runCatching {
        synchronized(lock) {
            check(!isClosed) { "Cache is closed" }

            map[key]?.let { node ->
                moveToTail(node)

                return@runCatching node.value
            }
        }

        val newInstance = factory(key)

        var instanceToClose: Value? = null

        val nodesToEvict = mutableListOf<Value>()

        try {
            synchronized(lock) {
                if (isClosed) {
                    instanceToClose = newInstance

                    error("Cache is closed")
                }

                when (val existingNode = map[key]) {
                    null -> {
                        while (map.size >= capacity && head != null) {
                            val oldestNode = head ?: break

                            detachNode(oldestNode)

                            map.remove(oldestNode.key)

                            nodesToEvict.add(oldestNode.value)
                        }

                        val newNode = Node(key = key, value = newInstance)

                        appendToTail(newNode)

                        map[key] = newNode

                        newInstance
                    }

                    else -> {
                        instanceToClose = newInstance

                        moveToTail(existingNode)

                        existingNode.value
                    }
                }
            }
        } finally {
            instanceToClose?.let(::closeResource)

            nodesToEvict.forEach(::closeResource)
        }
    }

    override fun remove(key: Key): Result<Unit> = runCatching {
        val valueToClose = synchronized(lock) {
            check(!isClosed) { "Cache is closed" }

            map.remove(key)?.also(::detachNode)?.value
        }

        valueToClose?.let(::closeResource)
    }

    override fun clear(): Result<Unit> = runCatching {
        val valuesToClose = synchronized(lock) {
            check(!isClosed) { "Cache is closed" }

            clearInternal()
        }

        valuesToClose.forEach(::closeResource)
    }

    override fun onRelease() {
        val valuesToClose = clearInternal()

        valuesToClose.forEach(::closeResource)
    }
}