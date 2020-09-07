package jjbridge.v8.inspector;

import jjbridge.v8.V8;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class V8MessageHandlerTest {
    private V8 v8;
    private V8MessageHandler messageHandler;

    @Before
    public final void before() {
        v8 = mock(V8.class);
        messageHandler = new V8MessageHandler(null, v8);
    }

    @Test
    public final void initNative() {
        int runtimeHandle = 123;

        messageHandler.initNative(runtimeHandle);
        verify(v8).initInspector(runtimeHandle, messageHandler);
    }

    @Test
    public final void sendToRuntime() {
        String message = "{\"message\":0,\"params\":{}}";

        messageHandler.sendToRuntime(message);
        verify(v8).onInspectorMessage(0, message);
    }

    @Test
    public final void close() {
        messageHandler.close();
        verify(v8).closeInspector(0);
    }

    @Test
    public final void close_afterInit() {
        int runtimeHandle = 62;
        long handle = 456;

        when(v8.initInspector(runtimeHandle, messageHandler)).thenReturn(handle);
        messageHandler.initNative(runtimeHandle);
        messageHandler.close();
        verify(v8).closeInspector(handle);
    }
}
