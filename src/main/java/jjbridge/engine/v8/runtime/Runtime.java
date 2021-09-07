package jjbridge.engine.v8.runtime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jjbridge.api.runtime.JSBaseRuntime;
import jjbridge.api.runtime.JSRuntime;
import jjbridge.api.value.JSArray;
import jjbridge.api.value.JSBoolean;
import jjbridge.api.value.JSDate;
import jjbridge.api.value.JSExternal;
import jjbridge.api.value.JSFunction;
import jjbridge.api.value.JSNull;
import jjbridge.api.value.JSNumber;
import jjbridge.api.value.JSObject;
import jjbridge.api.value.JSString;
import jjbridge.api.value.JSType;
import jjbridge.api.value.JSUndefined;
import jjbridge.api.value.JSValue;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.engine.utils.Cache;
import jjbridge.engine.utils.ReferenceMonitor;
import jjbridge.engine.v8.V8;

/**
 * The implementation of the {@link JSRuntime} using V8 runtime.
 */
public class Runtime extends JSBaseRuntime<Reference>
{
    private final V8 v8;
    private final long runtimeHandle;
    private final AccessorsFactory accessorsFactory;
    private final ReferenceMonitor<Reference> referenceMonitor;

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @SuppressFBWarnings(value = "SC_START_IN_CTOR",
            justification = "This class should be final but it is not due to mocking in tests")
    public Runtime(V8 v8, ReferenceMonitor<Reference> referenceMonitor,
                   Cache<FunctionCallback<Reference>> functionsCache,
                   Cache<ReferenceTypeGetter> typeGetterCache, Cache<EqualityChecker> equalityCheckerCache,
                   Cache<Object> externalCache)
    {
        super();
        this.v8 = v8;
        this.runtimeHandle = this.v8.createRuntime(this, functionsCache, typeGetterCache, equalityCheckerCache,
                externalCache);
        this.accessorsFactory = new AccessorsFactory(this.v8, this.runtimeHandle);
        this.referenceMonitor = referenceMonitor;
        this.referenceMonitor.start();
    }

    public long getNativeHandle()
    {
        return runtimeHandle;
    }

    @Override
    protected JSObject<Reference> getGlobalObject()
    {
        Reference ref = this.v8.globalObjectReference(this.runtimeHandle, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());
        return resolveReference(ref);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends JSValue> T resolve(Reference reference, JSType asType)
    {
        switch (asType)
        {
            case Undefined:
                return (T) new JSUndefined();
            case Null:
                return (T) new JSNull();
            case Boolean:
                return (T) new JSBoolean(accessorsFactory.booleanGetter(reference.handle),
                        accessorsFactory.booleanSetter(reference.handle));
            case Number:
                return (T) new JSNumber(accessorsFactory.doubleGetter(reference.handle),
                        accessorsFactory.doubleSetter(reference.handle));
            case String:
                return (T) new JSString(accessorsFactory.stringGetter(reference.handle),
                        accessorsFactory.stringSetter(reference.handle));
            case External:
                return (T) new JSExternal<>(accessorsFactory.externalGetter(reference.handle),
                        accessorsFactory.externalSetter(reference.handle));
            case Object:
                return (T) new JSObject<>(accessorsFactory.propertyGetter(reference.handle),
                        accessorsFactory.propertySetter(reference.handle));
            case Date:
                return (T) new JSDate<>(accessorsFactory.dateGetter(reference.handle),
                        accessorsFactory.dateSetter(reference.handle),
                        accessorsFactory.propertyGetter(reference.handle),
                        accessorsFactory.propertySetter(reference.handle));
            case Array:
                return (T) new JSArray<>(accessorsFactory.propertyGetter(reference.handle),
                        accessorsFactory.propertySetter(reference.handle),
                        accessorsFactory.arrayDataGetter(reference.handle),
                        accessorsFactory.arrayDataSetter(reference.handle));
            case Function:
                return (T) new JSFunction<>(Reference.class, accessorsFactory.propertyGetter(reference.handle),
                        accessorsFactory.propertySetter(reference.handle),
                        accessorsFactory.functionInvoker(reference.handle),
                        accessorsFactory.functionSetter(reference.handle));
            default:
                throw new UnsupportedOperationException("Cannot resolve reference of type " + asType.name());
        }
    }

    @Override
    protected Reference createNewReference(JSType type)
    {
        Reference reference = this.v8.newValue(this.runtimeHandle, type, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());

        switch (type)
        {
            case Undefined:
                this.v8.initUndefinedValue(this.runtimeHandle, reference.handle);
                break;
            case Null:
                this.v8.initNullValue(this.runtimeHandle, reference.handle);
                break;
            case Boolean:
                this.v8.initBooleanValue(this.runtimeHandle, reference.handle);
                break;
            case Number:
                this.v8.initDoubleValue(this.runtimeHandle, reference.handle);
                break;
            case String:
                this.v8.initStringValue(this.runtimeHandle, reference.handle);
                break;
            case External:
                this.v8.initExternalValue(this.runtimeHandle, reference.handle);
                break;
            case Object:
                this.v8.initObjectValue(this.runtimeHandle, reference.handle);
                break;
            case Date:
                this.v8.initDateTimeValue(this.runtimeHandle, reference.handle);
                break;
            case Array:
                this.v8.initArrayValue(this.runtimeHandle, reference.handle);
                break;
            case Function:
                this.v8.initFunctionValue(this.runtimeHandle, reference.handle);
                break;
            default:
                throw new UnsupportedOperationException("Cannot create reference of type " + type.name());
        }
        return reference;
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Called by native code")
    private void track(Reference reference)
    {
        /*
         We extract handle value here to avoid further references to object in lambda which will prevent garbage
         collection.
        */
        long handle = reference.handle;
        referenceMonitor.track(reference, () -> this.v8.releaseReference(this.runtimeHandle, handle));
    }

    @Override
    protected Reference runScript(String name, String script)
    {
        return this.v8.executeScript(this.runtimeHandle, name, script, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());
    }

    @Override
    public void close()
    {
        if (this.isClosed())
        {
            return;
        }

        this.referenceMonitor.interrupt();
        try
        {
            this.referenceMonitor.join();
        }
        catch (InterruptedException e)
        {
            // Ignored: we must release resources anyway
        }

        if (this.v8.releaseRuntime(this.runtimeHandle))
        {
            super.close();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Runtime))
        {
            return false;
        }
        return this.runtimeHandle == ((Runtime) obj).runtimeHandle;
    }

    @Override
    public int hashCode()
    {
        return (int) (runtimeHandle ^ (runtimeHandle >>> 32));
    }
}