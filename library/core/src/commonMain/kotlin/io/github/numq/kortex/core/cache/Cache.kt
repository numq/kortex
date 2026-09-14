package io.github.numq.kortex.core.cache

interface Cache<Key, Value> : AutoCloseable {
    fun getOrCreate(key: Key): Result<Value>

    fun remove(key: Key): Result<Unit>

    fun clear(): Result<Unit>
}