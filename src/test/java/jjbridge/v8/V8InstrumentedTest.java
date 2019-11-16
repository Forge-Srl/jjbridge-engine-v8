package jjbridge.v8;

import jjbridge.common.value.JSType;
import jjbridge.common.value.strategy.FunctionCallback;
import jjbridge.utils.Cache;
import jjbridge.utils.ReferenceMonitor;
import jjbridge.v8.runtime.EqualityChecker;
import jjbridge.v8.runtime.Reference;
import jjbridge.v8.runtime.ReferenceTypeGetter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class V8InstrumentedTest {
    private V8 v8;
    private long runtimeHandle;
    private ReferenceMonitor<Reference> referenceMonitor;
    private Cache<FunctionCallback<Reference>> functionCache;
    private Cache<ReferenceTypeGetter> typeGetterCache;
    private Cache<EqualityChecker> equalityCheckerCache;
    private Cache<Object> externalCache;

    @Before
    public void before() {
        v8 = spy(V8.class);
        referenceMonitor = spy(new ReferenceMonitor<>(50));
        functionCache = spy(new Cache<>());
        typeGetterCache = spy(new Cache<>());
        equalityCheckerCache = spy(new Cache<>());
        externalCache = spy(new Cache<>());

        runtimeHandle = v8.createRuntime(referenceMonitor, functionCache, typeGetterCache, equalityCheckerCache, externalCache);
        referenceMonitor.start();
    }

    @After
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
