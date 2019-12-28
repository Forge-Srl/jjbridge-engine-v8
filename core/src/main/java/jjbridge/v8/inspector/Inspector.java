package jjbridge.v8.inspector;

import jjbridge.common.inspector.Connection;
import jjbridge.common.inspector.JSBaseInspector;
import jjbridge.common.inspector.MessageHandler;
import jjbridge.v8.runtime.Runtime;
import jjbridge.v8.V8;

public class Inspector extends JSBaseInspector<Runtime> {
    private final V8 v8;

    public Inspector(int port, V8 v8) {
        super(port);
        this.v8 = v8;
    }

    @Override
    protected MessageHandler newMessageHandler(Connection connection, Runtime jsRuntime) {
        V8MessageHandler messageHandler = createMessageHandler(connection);
        messageHandler.initNative(jsRuntime.getNativeHandle());
        return messageHandler;
    }

    V8MessageHandler createMessageHandler(Connection connection) {
        return new V8MessageHandler(connection, v8);
    }
}
