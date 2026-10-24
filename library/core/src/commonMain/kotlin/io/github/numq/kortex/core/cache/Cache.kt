package io.github.numq.kortex.core.cache

/**
 * Generic key-value cache with automatic lifecycle resource management.
 */
interface Cache<Key, Value> : AutoCloseable {
    fun getOrCreate(key: Key): Result<Value>
    fun remove(key: Key): Result<Unit>
    fun clear(): Result<Unit>
}