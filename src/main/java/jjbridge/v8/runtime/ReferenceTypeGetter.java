package jjbridge.v8.runtime;

import jjbridge.common.value.JSType;

public interface ReferenceTypeGetter {
    JSType getType(long handle);
}
