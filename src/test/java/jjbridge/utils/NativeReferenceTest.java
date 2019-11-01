package jjbridge.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NativeReferenceTest {
    private NativeReference<Object> reference;
    private long id;
    private Object object;
    private CleanUpAction action;

    @Before
    public void before() {
        id = 123;
        object = new Object();
        action = mock(CleanUpAction.class);
        reference = new NativeReference<>(id, object, null, action);
    }

    @Test
    public void id() {
        assertEquals(id, reference.id);
    }

    @Test
    public void cleanUp() {
        reference.cleanUp();
        verify(action).cleanUp();
    }
}
