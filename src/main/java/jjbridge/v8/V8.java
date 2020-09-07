package jjbridge.v8;

import jjbridge.utils.NativeLibraryLoader;
import jjbridge.utils.ReferenceMonitor;
import jjbridge.v8.runtime.Reference;

public class V8 {
    static {
        NativeLibraryLoader.load("V8-wrapper");
    }

    private static V8 instance;
    protected V8() {} //not private because mockito sucks
    static V8 getInstance() {
        return instance != null ? instance : (instance = new V8());
    }

    //invoked by native code
    private static void track(long runtimeHandle, Reference reference, ReferenceMonitor<Reference> referenceMonitor) {
        // extracting handle value here to avoid further references to object in lambda which prevent garbage collection.
        long handle = reference.handle;
        referenceMonitor.track(reference, () -> releaseReference(runtimeHandle, handle));
    }

    static native void setFlags(String flags);

    private static native void releaseReference(long runtimeHandle, long referenceHandle);
    public native long createRuntime(Object referenceMonitor, Object functionsCache, Object typeGetterCache, Object equalityCheckerCache, Object externalCache);
    public native boolean releaseRuntime(long runtimeHandle);

    public native Object getReferenceType(long runtimeHandle, long referenceHandle);

    public native Object executeScript(long runtimeHandle, String fileName, String sourceCode, Object referenceTypeGetter, Object equalityChecker);

    public native Object globalObjectReference(long runtimeHandle, Object referenceTypeGetter, Object equalityChecker);

    public native Object newValue(long runtimeHandle, Object type, Object referenceTypeGetter, Object equalityChecker);
    public native boolean equalsValue(long runtimeHandle, long aValueHandle, long bValueHandle);

    public native void initUndefinedValue(long runtimeHandle, long valueHandle);
    public native void initNullValue(long runtimeHandle, long valueHandle);

    public native boolean getBooleanValue(long runtimeHandle, long valueHandle);
    public native void setBooleanValue(long runtimeHandle, long valueHandle, boolean value);
    public native void initBooleanValue(long runtimeHandle, long valueHandle);

    public native int getIntegerValue(long runtimeHandle, long valueHandle);
    public native void setIntegerValue(long runtimeHandle, long valueHandle, int value);
    public native void initIntegerValue(long runtimeHandle, long valueHandle);

    public native double getDoubleValue(long runtimeHandle, long valueHandle);
    public native void setDoubleValue(long runtimeHandle, long valueHandle, double value);
    public native void initDoubleValue(long runtimeHandle, long valueHandle);

    public native String getStringValue(long runtimeHandle, long valueHandle);
    public native void setStringValue(long runtimeHandle, long valueHandle, String value);
    public native void initStringValue(long runtimeHandle, long valueHandle);

    public native Object getExternalValue(long runtimeHandle, long valueHandle);
    public native void setExternalValue(long runtimeHandle, long valueHandle, Object value);
    public native void initExternalValue(long runtimeHandle, long valueHandle);

    public native Object getObjectProperty(long runtimeHandle, long objectHandle, String property, Object referenceTypeGetter, Object equalityChecker);
    public native void setObjectProperty(long runtimeHandle, long objectHandle, String property, long valueHandle);
    public native void initObjectValue(long runtimeHandle, long valueHandle);

    public native String getDateTimeString(long runtimeHandle, long objectHandle);
    public native void setDateTime(long runtimeHandle, long objectHandle, String dateTime);
    public native void initDateTimeValue(long runtimeHandle, long valueHandle);

    public native Object invokeFunction(long runtimeHandle, long functionHandle, long receiverHandle, long[] argHandles, Object referenceTypeGetter, Object equalityChecker);
    public native Object invokeConstructor(long runtimeHandle, long functionHandle, long[] argHandles, Object referenceTypeGetter, Object equalityChecker);
    public native void setFunctionHandler(long runtimeHandle, long functionHandle, Object handler, Object referenceTypeGetter, Object equalityChecker);
    public native void initFunctionValue(long runtimeHandle, long valueHandle);

    public native int getArraySize(long runtimeHandle, long arrayHandle);
    public native Object getElementByPosition(long runtimeHandle, long arrayHandle, int position, Object referenceTypeGetter, Object equalityChecker);
    public native void setElementByPosition(long runtimeHandle, long objectHandle, int position, long valueHandle);
    public native void initArrayValue(long runtimeHandle, long valueHandle);

    public native long initInspector(long runtimeHandle, Object messageHandler);
    public native void closeInspector(long inspectorHandle);
    public native void onInspectorMessage(long inspectorHandle, String message);
}
