package jjbridge.engine.v8.inspector;

import jjbridge.api.inspector.Connection;
import jjbridge.api.inspector.MessageHandler;
import jjbridge.engine.v8.V8;

class V8MessageHandler extends MessageHandler
{
    private final V8 v8;

    private long inspectorHandle;

    V8MessageHandler(Connection connection, V8 v8)
    {
        super(connection);
        this.v8 = v8;
    }

    void initNative(long runtimeHandle)
    {
        inspectorHandle = this.v8.initInspector(runtimeHandle, this);
    }

    @Override
    public void sendToRuntime(String s)
    {
        this.v8.onInspectorMessage(inspectorHandle, s);
    }

    @Override
    public void close()
    {
        this.v8.closeInspector(inspectorHandle);
    }
}
