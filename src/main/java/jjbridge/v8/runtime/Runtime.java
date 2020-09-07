package jjbridge.v8.runtime;

import jjbridge.common.runtime.JSBaseRuntime;
import jjbridge.common.value.*;
import jjbridge.utils.ReferenceMonitor;
import jjbridge.v8.V8;

public class Runtime extends JSBaseRuntime<Reference> {
    private final V8 v8;
    private final long runtimeHandle;
    private final AccessorsFactory accessorsFactory;
    private final ReferenceMonitor<Reference> referenceMonitor;

    public Runtime(V8 v8, long runtimeHandle, ReferenceMonitor<Reference> referenceMonitor) {
        super();
        this.v8 = v8;
        this.runtimeHandle = runtimeHandle;
        this.referenceMonitor = referenceMonitor;
        this.accessorsFactory = new AccessorsFactory(this.v8, this.runtimeHandle);

        this.referenceMonitor.start();
    }

    public long getNativeHandle() {
        return runtimeHandle;
    }

    @Override
    protected JSObject<Reference> getGlobalObject() {
        Reference ref = (Reference) this.v8.globalObjectReference(this.runtimeHandle, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());
        return resolveReference(ref);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends JSValue> T resolve(Reference reference, JSType asType) {
        switch (asType) {
            case Undefined:
                return (T) new JSUndefined();
            case Null:
                return (T) new JSNull();
            case Boolean:
                return (T) new JSBoolean(accessorsFactory.booleanGetter(reference.handle),
                        accessorsFactory.booleanSetter(reference.handle));
            case Integer:
                return (T) new JSInteger(accessorsFactory.integerGetter(reference.handle),
                        accessorsFactory.integerSetter(reference.handle));
            case Double:
                return (T) new JSDouble(accessorsFactory.doubleGetter(reference.handle),
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
    protected Reference createNewReference(JSType type) {
        Reference reference = (Reference) this.v8.newValue(this.runtimeHandle, type, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());

        switch (type) {
            case Undefined:
                this.v8.initUndefinedValue(this.runtimeHandle, reference.handle);
                break;
            case Null:
                this.v8.initNullValue(this.runtimeHandle, reference.handle);
                break;
            case Boolean:
                this.v8.initBooleanValue(this.runtimeHandle, reference.handle);
                break;
            case Integer:
                this.v8.initIntegerValue(this.runtimeHandle, reference.handle);
                break;
            case Double:
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

    @Override
    protected Reference runScript(String name, String script) {
        return (Reference) this.v8.executeScript(this.runtimeHandle, name, script, this.accessorsFactory.referenceTypeGetter(),
                this.accessorsFactory.equalityChecker());
    }

    @Override
    public void close() {
        if (this.isClosed()) return;
        this.referenceMonitor.interrupt();
        if (this.v8.releaseRuntime(this.runtimeHandle)) {
            super.close();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Runtime)) return false;
        return this.runtimeHandle == ((Runtime) obj).runtimeHandle;
    }
}