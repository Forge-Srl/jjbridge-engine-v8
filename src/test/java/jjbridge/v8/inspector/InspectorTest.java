package jjbridge.v8.inspector;

import jjbridge.v8.runtime.Runtime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InspectorTest {

    @Spy private final Inspector inspector = new Inspector(1000, null);
    @Mock private V8MessageHandler messageHandler;
    @Mock private Runtime runtime;

    @Test
    public final void newMessageHandler() {
        long handle = 123;

        when(inspector.createMessageHandler(null)).thenReturn(messageHandler);
        when(runtime.getNativeHandle()).thenReturn(handle);
        assertEquals(messageHandler, inspector.newMessageHandler(null, runtime));
        verify(messageHandler).initNative(handle);
    }
}
