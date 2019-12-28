package jjbridge.v8;

import jjbridge.common.JSEngine;
import jjbridge.common.inspector.JSInspector;
import jjbridge.common.runtime.JSRuntime;
import jjbridge.common.value.strategy.FunctionCallback;
import jjbridge.utils.Cache;
import jjbridge.utils.ReferenceMonitor;
import jjbridge.v8.inspector.Inspector;
import jjbridge.v8.runtime.EqualityChecker;
import jjbridge.v8.runtime.Reference;
import jjbridge.v8.runtime.ReferenceTypeGetter;
import jjbridge.v8.runtime.Runtime;

public class V8Engine implements JSEngine {
    public void setFlags(String[] flags) {
        StringBuilder sb = new StringBuilder();
        for (String flag : flags) {
            sb.append(flag).append(' ');
        }
        V8.setFlags(sb.toString());
    }

    @Override
    public JSRuntime newRuntime() {
        V8 v8 = V8.getInstance();
        ReferenceMonitor<Reference> referenceMonitor = new ReferenceMonitor<>(50);
        long runtimeHandle = v8.createRuntime(referenceMonitor, new Cache<FunctionCallback<Reference>>(),
                new Cache<ReferenceTypeGetter>(), new Cache<EqualityChecker>(), new Cache<>());
        return new Runtime(v8, runtimeHandle, referenceMonitor);
    }

    @Override
    public JSInspector newInspector(int port) {
        return new Inspector(port, V8.getInstance());
    }
}
