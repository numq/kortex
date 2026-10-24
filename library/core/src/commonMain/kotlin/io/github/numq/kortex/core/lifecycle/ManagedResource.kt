package io.github.numq.kortex.core.lifecycle

/**
 * An [AutoCloseable] resource that tracks its open/closed state.
 */
interface ManagedResource : AutoCloseable {
    val isClosed: Boolean

    fun checkOpen() = check(!isClosed) { "Resource is closed" }
}