package jjbridge.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CacheTest {
    private Cache<Object> cache;

    private final Object object1 = new Object();
    private final Object object2 = new Object();
    private final Object object3 = new Object();
    private final Object object4 = new Object();

    @BeforeEach
    public void before() {
        cache = new Cache<>();
    }

    @Test
    public void store() {
        int handle = 123;
        assertNull(cache.get(handle));
        cache.store(handle, object1);
        assertEquals(object1, cache.get(handle));
        cache.store(handle, object2);
        assertEquals(object2, cache.get(handle));
    }

    @Test
    public void delete() {
        int handle = 123;
        assertNull(cache.get(handle));
        cache.store(handle, object1);
        assertEquals(object1, cache.get(handle));
        cache.delete(handle);
        assertNull(cache.get(handle));
    }

    @Test
    public void clear() {
        int handle1 = 123;
        int handle2 = 104;
        int handle3 = 0;
        int handle4 = -73465;
        assertNull(cache.get(handle1));
        assertNull(cache.get(handle2));
        assertNull(cache.get(handle3));
        cache.store(handle1, object1);
        cache.store(handle2, object2);
        assertEquals(object1, cache.get(handle1));
        assertEquals(object2, cache.get(handle2));
        cache.store(handle3, object3);
        assertEquals(object3, cache.get(handle3));
        assertNull(cache.get(handle4));
        cache.store(handle4, object4);
        assertEquals(object4, cache.get(handle4));

        cache.clear();

        assertNull(cache.get(handle1));
        assertNull(cache.get(handle2));
        assertNull(cache.get(handle3));
        assertNull(cache.get(handle4));
    }
}
