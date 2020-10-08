package jjbridge.engine.v8;

import jjbridge.api.JSEngine;
import jjbridge.api.inspector.JSInspector;
import jjbridge.api.runtime.JSRuntime;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.engine.utils.Cache;
import jjbridge.engine.utils.ReferenceMonitor;
import jjbridge.engine.v8.inspector.Inspector;
import jjbridge.engine.v8.runtime.EqualityChecker;
import jjbridge.engine.v8.runtime.Reference;
import jjbridge.engine.v8.runtime.ReferenceTypeGetter;
import jjbridge.engine.v8.runtime.Runtime;

/**
 * The {@link JSEngine} implemented using <a href="https://v8.dev/">V8 JavaScript Engine</a>.
 * */
public final class V8Engine implements JSEngine
{
    /**
     * Pass additional flags to V8 engine.
     *
     * @param flags the list of flags
     * */
    public static void setFlags(String[] flags)
    {
        StringBuilder sb = new StringBuilder();
        for (String flag : flags)
        {
            sb.append(flag).append(' ');
        }
        V8.setFlags(sb.toString());
    }

    @Override
    public final JSRuntime newRuntime()
    {
        V8 v8 = V8.getInstance();
        ReferenceMonitor<Reference> referenceMonitor = new ReferenceMonitor<>(50);
        long runtimeHandle = v8.createRuntime(referenceMonitor, new Cache<FunctionCallback<Reference>>(),
                new Cache<ReferenceTypeGetter>(), new Cache<EqualityChecker>(), new Cache<>());
        return new Runtime(v8, runtimeHandle, referenceMonitor);
    }

    @Override
    public final JSInspector newInspector(int port)
    {
        return new Inspector(port, V8.getInstance());
    }
}
