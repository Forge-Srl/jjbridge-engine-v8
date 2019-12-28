package jjbridge.v8.inspector;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jjbridge.common.inspector.Connection;
import jjbridge.common.inspector.MessageHandler;
import jjbridge.v8.V8;

class V8MessageHandler extends MessageHandler {
    private final V8 v8;

    private long inspectorHandle;

    V8MessageHandler(Connection connection, V8 v8) {
        super(connection);
        this.v8 = v8;
    }

    void initNative(long runtimeHandle) {
        inspectorHandle = this.v8.initInspector(runtimeHandle, this);
    }

    @Override
    public void sendToRuntime(String s) {
        //workaround for messages that result in {"error":{"code":-32700,"message":"Message must be a valid JSON"}}
        JsonObject obj = new JsonParser().parse(s).getAsJsonObject();
        if (!obj.has("params")) {
            obj.add("params", new JsonObject());
            s = obj.toString();
        }

        this.v8.onInspectorMessage(inspectorHandle, s);
    }

    @Override
    public void close() {
        this.v8.closeInspector(inspectorHandle);
    }
}
