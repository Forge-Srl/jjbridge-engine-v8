package jjbridge.v8.inspector;

import jjbridge.v8.runtime.Runtime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class InspectorTest {
    @Test
    public final void newMessageHandler() {
        long handle = 123;
        Inspector inspector = spy(new Inspector(1000, null));
        V8MessageHandler messageHandler = mock(V8MessageHandler.class);
        Runtime runtime = mock(Runtime.class);

        when(inspector.createMessageHandler(null)).thenReturn(messageHandler);
        when(runtime.getNativeHandle()).thenReturn(handle);
        assertEquals(messageHandler, inspector.newMessageHandler(null, runtime));
        verify(messageHandler).initNative(handle);
    }
}
