package jjbridge.engine.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NativeReferenceTest {
    private static final long id = 123;
    private NativeReference<Object> reference;
    @Mock private CleanUpAction action;

    @BeforeEach
    public void before() {
        reference = new NativeReference<>(id, new Object(), null, action);
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
