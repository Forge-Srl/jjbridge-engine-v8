package jjbridge.engine.utils;

import java.util.HashMap;

/**
 * This class allows caching objects of the given type.
 *
 * @param <T> the type of the objects to cache
 * */
public class Cache<T>
{
    private final HashMap<Long, T> callbackCache;

    public Cache()
    {
        this.callbackCache = new HashMap<>();
    }

    /**
     * Stores an object in the cache using the given handle for later retrieval.
     *
     * @param handle the handle used for retrieval
     * @param value the value to store
     * */
    public void store(long handle, T value)
    {
        this.callbackCache.put(handle, value);
    }

    /**
     * Gets an object with the given handle from the cache, or {@code null} if the is no object for that handle.
     *
     * @param handle the handle used for retrieval
     * @return the object previously stored if available, or {@code null} otherwise.
     * */
    public T get(long handle)
    {
        return this.callbackCache.get(handle);
    }

    /**
     * Removes the object with the given handle from the cache.
     *
     * @param handle the handle used for retrieval
     * */
    public void delete(long handle)
    {
        this.callbackCache.remove(handle);
    }

    /**
     * Removes all objects from the cache.
     * */
    public void clear()
    {
        this.callbackCache.clear();
    }
}
