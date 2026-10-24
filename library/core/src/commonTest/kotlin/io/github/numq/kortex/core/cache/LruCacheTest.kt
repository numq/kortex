package io.github.numq.kortex.core.cache

import kotlin.test.*

class LruCacheTest {
    private class TestCloseable(val id: String) : AutoCloseable {
        var isClosed = false
        override fun close() {
            isClosed = true
        }
    }

    private class TestCache(capacity: Int) : LruCache<String, TestCloseable>(capacity) {
        var createCount = 0
        override val factory: String.() -> TestCloseable = {
            createCount++
            TestCloseable(this)
        }
    }

    @Test
    fun `returns cached value without recreating`() {
        val cache = TestCache(capacity = 3)

        val first = cache.getOrCreate("key1").getOrThrow()
        val second = cache.getOrCreate("key1").getOrThrow()

        assertSame(first, second)
        assertEquals(1, cache.createCount)
    }

    @Test
    fun `evicts least recently used entry and closes it when capacity is exceeded`() {
        val cache = TestCache(capacity = 2)

        val item1 = cache.getOrCreate("key1").getOrThrow()
        val item2 = cache.getOrCreate("key2").getOrThrow()

        cache.getOrCreate("key1").getOrThrow()

        val item3 = cache.getOrCreate("key3").getOrThrow()

        assertTrue(item2.isClosed, "item2 should be closed upon eviction")
        assertFalse(item1.isClosed, "item1 should remain active in cache")
        assertFalse(item3.isClosed, "item3 should remain active in cache")
    }

    @Test
    fun `clear closes all cached entries`() {
        val cache = TestCache(capacity = 3)

        val item1 = cache.getOrCreate("key1").getOrThrow()
        val item2 = cache.getOrCreate("key2").getOrThrow()

        cache.clear().getOrThrow()

        assertTrue(item1.isClosed)
        assertTrue(item2.isClosed)

        val item1New = cache.getOrCreate("key1").getOrThrow()
        assertNotSame(item1, item1New)
    }

    @Test
    fun `cannot get or create after close`() {
        val cache = TestCache(capacity = 2)
        val item = cache.getOrCreate("key1").getOrThrow()

        cache.close()

        assertTrue(item.isClosed)
        assertTrue(cache.isClosed)
        assertTrue(cache.getOrCreate("key2").isFailure)
    }
}