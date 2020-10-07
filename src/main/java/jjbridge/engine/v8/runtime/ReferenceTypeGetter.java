package jjbridge.engine.v8.runtime;

import jjbridge.api.value.JSType;

public interface ReferenceTypeGetter
{
    JSType getType(long handle);
}
