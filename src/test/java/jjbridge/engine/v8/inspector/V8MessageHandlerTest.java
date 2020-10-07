package jjbridge.engine.v8.inspector;

import jjbridge.engine.v8.V8;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class V8MessageHandlerTest {
    @Mock private V8 v8;
    private V8MessageHandler messageHandler;

    @BeforeEach
    public final void before() {
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
