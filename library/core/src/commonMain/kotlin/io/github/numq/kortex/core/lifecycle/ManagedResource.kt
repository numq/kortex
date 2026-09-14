package io.github.numq.kortex.core.lifecycle

interface ManagedResource : AutoCloseable {
    val isClosed: Boolean

    fun checkOpen() = check(!isClosed) { "Resource is closed" }
}