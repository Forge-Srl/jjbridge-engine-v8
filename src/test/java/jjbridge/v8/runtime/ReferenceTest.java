package jjbridge.v8.runtime;

import jjbridge.common.value.JSType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ReferenceTest {
    private Reference reference;
    private JSType jsType;
    private ReferenceTypeGetter referenceTypeGetter;
    private EqualityChecker equalityChecker;
    private int handle;

    @Before
    public void before() {
        handle = 1;
        jsType = JSType.Null;
        referenceTypeGetter = h -> {
            if (h == handle) return JSType.External;
            throw new IllegalArgumentException();
        };
        equalityChecker = (aHandle, bHandle) -> aHandle == bHandle;
        reference = new Reference(handle, jsType, referenceTypeGetter, equalityChecker);
    }

    @Test
    public void ctor() {
        assertEquals(reference, reference);
        assertEquals(new Reference(handle, jsType, referenceTypeGetter, equalityChecker), reference);
        assertNotEquals(null, reference);
        assertNotEquals(new Reference(handle+3, jsType, referenceTypeGetter, equalityChecker), reference);
        assertNotEquals(new Reference(handle, jsType, h -> null, (aHandle, bHandle) -> false), reference);
    }

    @Test
    public void getNominalType() {
        assertEquals(jsType, reference.getNominalType());
    }

    @Test
    public void getActualType() {
        assertEquals(referenceTypeGetter.getType(handle), reference.getActualType());
    }
}
