package jjbridge.utils;

import java.util.HashMap;

public class Cache<T>
{
    private final HashMap<Long, T> callbackCache;

    public Cache()
    {
        this.callbackCache = new HashMap<>();
    }

    public void store(long handle, T value)
    {
        this.callbackCache.put(handle, value);
    }

    public T get(long handle)
    {
        return this.callbackCache.get(handle);
    }

    public void delete(long handle)
    {
        this.callbackCache.remove(handle);
    }

    public void clear()
    {
        this.callbackCache.clear();
    }
}
