package jjbridge.v8;

import jjbridge.common.value.JSType;
import jjbridge.common.value.strategy.FunctionCallback;
import jjbridge.utils.Cache;
import jjbridge.utils.ReferenceMonitor;
import jjbridge.v8.runtime.EqualityChecker;
import jjbridge.v8.runtime.Reference;
import jjbridge.v8.runtime.ReferenceTypeGetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class V8Test {
    private long runtimeHandle;

    @Spy private V8 v8;
    @Spy private ReferenceMonitor<Reference> referenceMonitor = new ReferenceMonitor<>(50);
    @Spy private Cache<FunctionCallback<Reference>> functionCache;
    @Spy private Cache<ReferenceTypeGetter> typeGetterCache;
    @Spy private Cache<EqualityChecker> equalityCheckerCache;
    @Spy private Cache<Object> externalCache;

    @BeforeEach
    public void before() {
        runtimeHandle = v8.createRuntime(referenceMonitor, functionCache, typeGetterCache, equalityCheckerCache, externalCache);
        referenceMonitor.start();
    }

    @AfterEach
    public void after() {
        referenceMonitor.interrupt();
        v8.releaseRuntime(runtimeHandle);
    }

    @Test
    public void newReferencesAreTracked() {
        for (int i = 0; i < 100; i++) {
            ReferenceTypeGetter referenceTypeGetter = handle -> JSType.Integer;
            EqualityChecker equalityChecker = (a,b) -> a == b;
            Reference ref = (Reference) v8.newValue(runtimeHandle, JSType.Integer, referenceTypeGetter, equalityChecker);
            verify(referenceMonitor).track(eq(ref), any());
        }
    }

    @Test
    public void externalReferenceUsesExternalCache() {
        ReferenceTypeGetter referenceTypeGetter = handle -> JSType.External;
        EqualityChecker equalityChecker = (a,b) -> true;
        Reference ref = (Reference) v8.newValue(runtimeHandle, JSType.External, referenceTypeGetter, equalityChecker);
        verify(referenceMonitor).track(eq(ref), any());

        long handle = ref.handle;
        v8.initExternalValue(runtimeHandle, handle);

        Object value = new Object();
        v8.setExternalValue(runtimeHandle, handle, value);
        verify(externalCache).store(handle, value);

        Object obj = v8.getExternalValue(runtimeHandle, handle);
        assertEquals(value, obj);

        // setting again to test removal of previous value from cache
        Object value2 = new Object[] {"Something", false, 153.0000978, null, new Object()};
        v8.setExternalValue(runtimeHandle, handle, value2);
        verify(externalCache).delete(handle);
        verify(externalCache).store(handle, value2);

        obj = v8.getExternalValue(runtimeHandle, handle);
        assertEquals(value2, obj);
    }

    @Test
    public void functionReferenceUsesExternalCache() {
        ReferenceTypeGetter referenceTypeGetter = handle -> JSType.Function;
        EqualityChecker equalityChecker = (a,b) -> true;
        Reference ref = (Reference) v8.newValue(runtimeHandle, JSType.Function, referenceTypeGetter, equalityChecker);
        verify(referenceMonitor).track(eq(ref), any());

        long handle = ref.handle;
        v8.initFunctionValue(runtimeHandle, handle);

        Reference callbackResult = (Reference) v8.newValue(runtimeHandle, JSType.String, referenceTypeGetter, equalityChecker);
        v8.initStringValue(runtimeHandle, callbackResult.handle);

        FunctionCallback<Reference> callback = arguments -> callbackResult;
        v8.setFunctionHandler(runtimeHandle, handle, callback, referenceTypeGetter, equalityChecker);
        verify(functionCache).store(handle, callback);
        verify(typeGetterCache).store(handle, referenceTypeGetter);
        verify(equalityCheckerCache).store(handle, equalityChecker);

        Reference obj = (Reference) v8.invokeFunction(runtimeHandle, handle, handle, new long[0], referenceTypeGetter, equalityChecker);
        assertEquals(callbackResult.getNominalType(), obj.getNominalType());

        // setting again to test removal of previous value from cache
        Reference callbackResult2 = (Reference) v8.newValue(runtimeHandle, JSType.Date, referenceTypeGetter, equalityChecker);
        v8.initDateTimeValue(runtimeHandle, callbackResult2.handle);

        FunctionCallback<Reference> callback2 = arguments -> callbackResult2;
        v8.setFunctionHandler(runtimeHandle, handle, callback2, null, null);
        verify(functionCache).delete(handle);
        verify(functionCache).store(handle, callback2);
        verify(typeGetterCache).delete(handle);
        verify(typeGetterCache).store(handle, null);
        verify(equalityCheckerCache).delete(handle);
        verify(equalityCheckerCache).store(handle, null);

        obj = (Reference) v8.invokeFunction(runtimeHandle, handle, handle, new long[0], referenceTypeGetter, equalityChecker);
        assertEquals(callbackResult2.getNominalType(), obj.getNominalType());
    }
}
