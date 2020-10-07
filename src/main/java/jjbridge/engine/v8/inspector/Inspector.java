package jjbridge.engine.v8.inspector;

import jjbridge.api.inspector.Connection;
import jjbridge.api.inspector.JSBaseInspector;
import jjbridge.api.inspector.MessageHandler;
import jjbridge.engine.v8.V8;
import jjbridge.engine.v8.runtime.Runtime;

public class Inspector extends JSBaseInspector<Runtime>
{
    private final V8 v8;

    public Inspector(int port, V8 v8)
    {
        super(port);
        this.v8 = v8;
    }

    @Override
    protected MessageHandler newMessageHandler(Connection connection, Runtime jsRuntime)
    {
        V8MessageHandler messageHandler = createMessageHandler(connection);
        messageHandler.initNative(jsRuntime.getNativeHandle());
        return messageHandler;
    }

    V8MessageHandler createMessageHandler(Connection connection)
    {
        return new V8MessageHandler(connection, v8);
    }
}
