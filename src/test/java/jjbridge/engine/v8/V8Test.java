package jjbridge.engine.v8;

import jjbridge.api.value.JSType;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.engine.utils.Cache;
import jjbridge.engine.utils.ReferenceMonitor;
import jjbridge.engine.v8.runtime.EqualityChecker;
import jjbridge.engine.v8.runtime.Reference;
import jjbridge.engine.v8.runtime.ReferenceTypeGetter;
import jjbridge.engine.v8.runtime.Runtime;
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
    @Spy private V8 v8;
    @Spy private final ReferenceMonitor<Reference> referenceMonitor = new ReferenceMonitor<>(50);
    @Spy private Cache<FunctionCallback<Reference>> functionCache;
    @Spy private Cache<ReferenceTypeGetter> typeGetterCache;
    @Spy private Cache<EqualityChecker> equalityCheckerCache;
    @Spy private Cache<Object> externalCache;
    private Runtime runtime;

    @BeforeEach
    public void before() {
        v8 = V8.getInstance();
        runtime = new Runtime(v8, referenceMonitor, functionCache, typeGetterCache, equalityCheckerCache, externalCache);
    }

    @AfterEach
    public void after() {
        runtime.close();
    }

    @Test
    public void newReferencesAreTracked() {
        for (int i = 0; i < 100; i++) {
            ReferenceTypeGetter referenceTypeGetter = handle -> JSType.Number;
            EqualityChecker equalityChecker = (a,b) -> a == b;
            Reference ref = v8.newValue(runtime.getNativeHandle(), JSType.Number, referenceTypeGetter, equalityChecker);
            verify(referenceMonitor).track(eq(ref), any());
        }
    }

    @Test
    public void externalReferenceUsesExternalCache() {
        ReferenceTypeGetter referenceTypeGetter = handle -> JSType.External;
        EqualityChecker equalityChecker = (a,b) -> true;
        Reference ref = v8.newValue(runtime.getNativeHandle(), JSType.External, referenceTypeGetter, equalityChecker);
        verify(referenceMonitor).track(eq(ref), any());

        long handle = ref.handle;
        v8.initExternalValue(runtime.getNativeHandle(), handle);

        Object value = new Object();
        v8.setExternalValue(runtime.getNativeHandle(), handle, value);
        verify(externalCache).store(handle, value);

        Object obj = v8.getExternalValue(runtime.getNativeHandle(), handle);
        assertEquals(value, obj);

        // setting again to test removal of previous value from cache
        Object value2 = new Object[] {"Something", false, 153.0000978, null, new Object()};
        v8.setExternalValue(runtime.getNativeHandle(), handle, value2);
        verify(externalCache).delete(handle);
        verify(externalCache).store(handle, value2);

        obj = v8.getExternalValue(runtime.getNativeHandle(), handle);
        assertEquals(value2, obj);
    }

    @Test
    public void functionReferenceUsesExternalCache() {
        ReferenceTypeGetter referenceTypeGetter = handle -> JSType.Function;
        EqualityChecker equalityChecker = (a,b) -> true;
        Reference ref = v8.newValue(runtime.getNativeHandle(), JSType.Function, referenceTypeGetter, equalityChecker);
        verify(referenceMonitor).track(eq(ref), any());

        long handle = ref.handle;
        v8.initFunctionValue(runtime.getNativeHandle(), handle);

        Reference callbackResult = v8.newValue(runtime.getNativeHandle(), JSType.String, referenceTypeGetter, equalityChecker);
        v8.initStringValue(runtime.getNativeHandle(), callbackResult.handle);

        FunctionCallback<Reference> callback = arguments -> callbackResult;
        v8.setFunctionHandler(runtime.getNativeHandle(), handle, callback, referenceTypeGetter, equalityChecker);
        verify(functionCache).store(handle, callback);
        verify(typeGetterCache).store(handle, referenceTypeGetter);
        verify(equalityCheckerCache).store(handle, equalityChecker);

        Reference obj = v8.invokeFunction(runtime.getNativeHandle(), handle, handle, new long[0], referenceTypeGetter, equalityChecker);
        assertEquals(callbackResult.getNominalType(), obj.getNominalType());

        // setting again to test removal of previous value from cache
        Reference callbackResult2 = v8.newValue(runtime.getNativeHandle(), JSType.Date, referenceTypeGetter, equalityChecker);
        v8.initDateTimeValue(runtime.getNativeHandle(), callbackResult2.handle);

        FunctionCallback<Reference> callback2 = arguments -> callbackResult2;
        v8.setFunctionHandler(runtime.getNativeHandle(), handle, callback2, null, null);
        verify(functionCache).delete(handle);
        verify(functionCache).store(handle, callback2);
        verify(typeGetterCache).delete(handle);
        verify(typeGetterCache).store(handle, null);
        verify(equalityCheckerCache).delete(handle);
        verify(equalityCheckerCache).store(handle, null);

        obj = v8.invokeFunction(runtime.getNativeHandle(), handle, handle, new long[0], referenceTypeGetter, equalityChecker);
        assertEquals(callbackResult2.getNominalType(), obj.getNominalType());
    }
}
