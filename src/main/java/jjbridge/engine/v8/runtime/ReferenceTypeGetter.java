package jjbridge.engine.v8.runtime;

import jjbridge.api.value.JSType;

/**
 * Retrieve the type of a JavaScript object.
 * */
public interface ReferenceTypeGetter
{
    /**
     * Gets the type of a JavaScript object with the given handle.
     *
     * @param handle the handle associated to the JavaScript object
     * @return the type of the JavaScript object
     * */
    JSType getType(long handle);
}
