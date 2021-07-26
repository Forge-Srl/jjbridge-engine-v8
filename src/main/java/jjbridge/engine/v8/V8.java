package jjbridge.engine.v8;

import jjbridge.api.inspector.MessageHandler;
import jjbridge.api.value.JSType;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.engine.utils.Cache;
import jjbridge.engine.utils.NativeLibraryLoader;
import jjbridge.engine.v8.runtime.EqualityChecker;
import jjbridge.engine.v8.runtime.Reference;
import jjbridge.engine.v8.runtime.ReferenceTypeGetter;
import jjbridge.engine.v8.runtime.Runtime;

@SuppressWarnings({"checkstyle:MissingJavadocType", "checkstyle:MissingJavadocMethod"})
public class V8
{
    private static final NativeLibraryLoader nativeLibraryLoader = new NativeLibraryLoader();
    private static V8 instance;
    private final Object lock = new Object();

    static
    {
        nativeLibraryLoader.loadLibrary("V8-wrapper", new String[]{"icuuc"});
    }

    // Not private because mockito sucks
    protected V8()
    {
    }

    static synchronized V8 getInstance()
    {
        if (instance != null)
        {
            return instance;
        }

        String resourcePath = nativeLibraryLoader.getResourcePath("icudtl.dat");
        if (!initializeV8_internal(resourcePath))
        {
            throw new RuntimeException("Cannot initialize V8 with " + resourcePath);
        }
        return instance = new V8();
    }

    static void setAssetLoader(NativeLibraryLoader.AssetLoader loader)
    {
        nativeLibraryLoader.setAssetLoader(loader);
    }

    private static native void setFlags_internal(String flags);

    static void setFlags(String[] flags)
    {
        StringBuilder sb = new StringBuilder();
        for (String flag : flags)
        {
            sb.append(flag).append(' ');
        }
        setFlags_internal(sb.toString());
    }

    private static native boolean initializeV8_internal(String resourcePath);

    private native long createRuntime_internal(Object runtime, Object functionsCache, Object typeGetterCache,
                                               Object equalityCheckerCache, Object externalCache);

    public long createRuntime(Runtime runtime, Cache<FunctionCallback<Reference>> functionsCache,
                              Cache<ReferenceTypeGetter> typeGetterCache, Cache<EqualityChecker> equalityCheckerCache,
                              Cache<Object> externalCache)
    {
        return createRuntime_internal(runtime, functionsCache, typeGetterCache, equalityCheckerCache, externalCache);
    }

    private native boolean releaseRuntime_internal(long runtimeHandle);

    public boolean releaseRuntime(long runtimeHandle)
    {
        return releaseRuntime_internal(runtimeHandle);
    }

    private native void releaseReference_internal(long runtimeHandle, long referenceHandle);

    public void releaseReference(long runtimeHandle, long referenceHandle)
    {
        releaseReference_internal(runtimeHandle, referenceHandle);
    }

    private native Object getReferenceType_internal(long runtimeHandle, long referenceHandle);

    public JSType getReferenceType(long runtimeHandle, long referenceHandle)
    {
        synchronized (lock)
        {
            return (JSType) getReferenceType_internal(runtimeHandle, referenceHandle);
        }
    }

    private native Object executeScript_internal(long runtimeHandle, String fileName, String sourceCode,
                                                 Object referenceTypeGetter, Object equalityChecker);

    public Reference executeScript(long runtimeHandle, String fileName, String sourceCode,
                                   ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) executeScript_internal(runtimeHandle, fileName, sourceCode, referenceTypeGetter,
                    equalityChecker);
        }
    }

    private native Object globalObjectReference_internal(long runtimeHandle, Object referenceTypeGetter,
                                                         Object equalityChecker);

    public Reference globalObjectReference(long runtimeHandle, ReferenceTypeGetter referenceTypeGetter,
                                            EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) globalObjectReference_internal(runtimeHandle, referenceTypeGetter, equalityChecker);
        }
    }

    private native Object newValue_internal(long runtimeHandle, Object type, Object referenceTypeGetter,
                                            Object equalityChecker);

    public Reference newValue(long runtimeHandle, JSType type, ReferenceTypeGetter referenceTypeGetter,
                              EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) newValue_internal(runtimeHandle, type, referenceTypeGetter, equalityChecker);
        }
    }

    private native boolean equalsValue_internal(long runtimeHandle, long firstValueHandle, long secondValueHandle);

    public boolean equalsValue(long runtimeHandle, long firstValueHandle, long secondValueHandle)
    {
        synchronized (lock)
        {
            return equalsValue_internal(runtimeHandle, firstValueHandle, secondValueHandle);
        }
    }

    private native void initUndefinedValue_internal(long runtimeHandle, long valueHandle);

    public void initUndefinedValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initUndefinedValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void initNullValue_internal(long runtimeHandle, long valueHandle);

    public void initNullValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initNullValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native boolean getBooleanValue_internal(long runtimeHandle, long valueHandle);

    public boolean getBooleanValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return getBooleanValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setBooleanValue_internal(long runtimeHandle, long valueHandle, boolean value);

    public void setBooleanValue(long runtimeHandle, long valueHandle, boolean value)
    {
        synchronized (lock)
        {
            setBooleanValue_internal(runtimeHandle, valueHandle, value);
        }
    }

    private native void initBooleanValue_internal(long runtimeHandle, long valueHandle);

    public void initBooleanValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initBooleanValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native long getLongValue_internal(long runtimeHandle, long valueHandle);

    public long getLongValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return getLongValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setLongValue_internal(long runtimeHandle, long valueHandle, long value);

    public void setLongValue(long runtimeHandle, long valueHandle, long value)
    {
        synchronized (lock)
        {
            setLongValue_internal(runtimeHandle, valueHandle, value);
        }
    }

    private native void initLongValue_internal(long runtimeHandle, long valueHandle);

    public void initLongValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initLongValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native double getDoubleValue_internal(long runtimeHandle, long valueHandle);

    public double getDoubleValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return getDoubleValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setDoubleValue_internal(long runtimeHandle, long valueHandle, double value);

    public void setDoubleValue(long runtimeHandle, long valueHandle, double value)
    {
        synchronized (lock)
        {
            setDoubleValue_internal(runtimeHandle, valueHandle, value);
        }
    }

    private native void initDoubleValue_internal(long runtimeHandle, long valueHandle);

    public void initDoubleValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initDoubleValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native String getStringValue_internal(long runtimeHandle, long valueHandle);

    public String getStringValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return getStringValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setStringValue_internal(long runtimeHandle, long valueHandle, String value);

    public void setStringValue(long runtimeHandle, long valueHandle, String value)
    {
        synchronized (lock)
        {
            setStringValue_internal(runtimeHandle, valueHandle, value);
        }
    }

    private native void initStringValue_internal(long runtimeHandle, long valueHandle);

    public void initStringValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initStringValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native Object getExternalValue_internal(long runtimeHandle, long valueHandle);

    @SuppressWarnings("unchecked")
    public <T> T getExternalValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return (T) getExternalValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setExternalValue_internal(long runtimeHandle, long valueHandle, Object value);

    public <T> void setExternalValue(long runtimeHandle, long valueHandle, T value)
    {
        synchronized (lock)
        {
            setExternalValue_internal(runtimeHandle, valueHandle, value);
        }
    }

    private native void initExternalValue_internal(long runtimeHandle, long valueHandle);

    public void initExternalValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initExternalValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native Object getObjectProperty_internal(long runtimeHandle, long objectHandle, String property,
                                           Object referenceTypeGetter, Object equalityChecker);

    public Reference getObjectProperty(long runtimeHandle, long objectHandle, String property,
                                       ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) getObjectProperty_internal(runtimeHandle, objectHandle, property, referenceTypeGetter,
                    equalityChecker);
        }
    }

    private native void setObjectProperty_internal(long runtimeHandle, long objectHandle, String property,
                                                   long valueHandle);

    public void setObjectProperty(long runtimeHandle, long objectHandle, String property, long valueHandle)
    {
        synchronized (lock)
        {
            setObjectProperty_internal(runtimeHandle, objectHandle, property, valueHandle);
        }
    }

    private native void initObjectValue_internal(long runtimeHandle, long valueHandle);

    public void initObjectValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initObjectValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native String getDateTimeString_internal(long runtimeHandle, long objectHandle);

    public String getDateTimeString(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            return getDateTimeString_internal(runtimeHandle, valueHandle);
        }
    }

    private native void setDateTimeString_internal(long runtimeHandle, long objectHandle, String dateTime);

    public void setDateTimeString(long runtimeHandle, long objectHandle, String value)
    {
        synchronized (lock)
        {
            setDateTimeString_internal(runtimeHandle, objectHandle, value);
        }
    }

    private native void initDateTimeValue_internal(long runtimeHandle, long valueHandle);

    public void initDateTimeValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initDateTimeValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native Object invokeFunction_internal(long runtimeHandle, long functionHandle, long receiverHandle,
                                                  long[] argHandles, Object referenceTypeGetter,
                                                  Object equalityChecker);

    public Reference invokeFunction(long runtimeHandle, long functionHandle, long receiverHandle, long[] argHandles,
                                    ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) invokeFunction_internal(runtimeHandle, functionHandle, receiverHandle, argHandles,
                    referenceTypeGetter, equalityChecker);
        }
    }

    private native Object invokeConstructor_internal(long runtimeHandle, long functionHandle, long[] argHandles,
                                           Object referenceTypeGetter, Object equalityChecker);

    public Reference invokeConstructor(long runtimeHandle, long functionHandle, long[] argHandles,
                                    ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) invokeConstructor_internal(runtimeHandle, functionHandle, argHandles,
                    referenceTypeGetter, equalityChecker);
        }
    }

    private native void setFunctionHandler_internal(long runtimeHandle, long functionHandle, Object handler,
                                          Object referenceTypeGetter, Object equalityChecker);

    public void setFunctionHandler(long runtimeHandle, long functionHandle, FunctionCallback<Reference> handler,
                                       ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            setFunctionHandler_internal(runtimeHandle, functionHandle, handler, referenceTypeGetter, equalityChecker);
        }
    }

    private native void initFunctionValue_internal(long runtimeHandle, long valueHandle);

    public void initFunctionValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initFunctionValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native int getArraySize_internal(long runtimeHandle, long arrayHandle);

    public int getArraySize(long runtimeHandle, long arrayHandle)
    {
        synchronized (lock)
        {
            return getArraySize_internal(runtimeHandle, arrayHandle);
        }
    }

    private native Object getElementByPosition_internal(long runtimeHandle, long arrayHandle, int position,
                                                        Object referenceTypeGetter, Object equalityChecker);

    public Reference getElementByPosition(long runtimeHandle, long arrayHandle, int position,
                                          ReferenceTypeGetter referenceTypeGetter, EqualityChecker equalityChecker)
    {
        synchronized (lock)
        {
            return (Reference) getElementByPosition_internal(runtimeHandle, arrayHandle, position, referenceTypeGetter,
                    equalityChecker);
        }
    }

    private native void setElementByPosition_internal(long runtimeHandle, long arrayHandle, int position,
                                                      long valueHandle);

    public void setElementByPosition(long runtimeHandle, long arrayHandle, int position, long valueHandle)
    {
        synchronized (lock)
        {
            setElementByPosition_internal(runtimeHandle, arrayHandle, position, valueHandle);
        }
    }

    private native void initArrayValue_internal(long runtimeHandle, long valueHandle);

    public void initArrayValue(long runtimeHandle, long valueHandle)
    {
        synchronized (lock)
        {
            initArrayValue_internal(runtimeHandle, valueHandle);
        }
    }

    private native long initInspector_internal(long runtimeHandle, Object messageHandler);

    public long initInspector(long runtimeHandle, MessageHandler messageHandler)
    {
        return initInspector_internal(runtimeHandle, messageHandler);
    }

    private native void closeInspector_internal(long inspectorHandle);

    public void closeInspector(long inspectorHandle)
    {
        closeInspector_internal(inspectorHandle);
    }

    private native void onInspectorMessage_internal(long inspectorHandle, String message);

    public void onInspectorMessage(long inspectorHandle, String message)
    {
        onInspectorMessage_internal(inspectorHandle, message);
    }
}
