package io.github.numq.kortex.core.lifecycle

import kotlinx.atomicfu.atomic

/**
 * Base class for thread-safe resources that can be closed exactly once.
 */
abstract class CloseableResource : ManagedResource {
    private val _isClosed = atomic(false)

    override val isClosed: Boolean get() = _isClosed.value

    protected abstract fun onRelease()

    final override fun close() {
        if (_isClosed.compareAndSet(expect = false, update = true)) {
            onRelease()
        }
    }
}