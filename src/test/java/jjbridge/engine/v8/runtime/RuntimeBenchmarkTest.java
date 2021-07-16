package jjbridge.engine.v8.runtime;

import jjbridge.engine.MemoryTimeWaster;
import jjbridge.api.runtime.JSReference;
import jjbridge.api.runtime.JSRuntime;
import jjbridge.api.value.*;
import jjbridge.engine.v8.V8Engine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class RuntimeBenchmarkTest
{
    private V8Engine engine;

    @BeforeEach
    public void before() {
        engine = new V8Engine();
    }

    @Test
    public void memoryLeak_many_runtime() {
        while (true) {
            try(JSRuntime runtime = engine.newRuntime()) {
                memoryLeakAction(runtime);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
            MemoryTimeWaster.waste(1000000);
        }
    }

    @Test
    public void memoryLeak_single_runtime() {
        boolean x = true;

        try (JSRuntime runtime = engine.newRuntime())
        {
            while(x) {
                memoryLeakAction(runtime);
                MemoryTimeWaster.waste(1000000);
                System.gc();
                Thread.sleep(1500);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void memoryLeakAction(JSRuntime runtime) {
        final String stringToCheck = "some string to check";
        final long numberToCheck = 9856214;

        JSReference arg1 = runtime.newReference(JSType.Null);
        JSReference arg2 = runtime.newReference(JSType.String);

        JSString arg2String = runtime.resolveReference(arg2);
        arg2String.setValue(stringToCheck);

        JSReference functionRef = runtime.newReference(JSType.Function);
        JSFunction function = runtime.resolveReference(functionRef);

        boolean[] callbackResult = {false, false, false};
        {
            function.setFunction(arguments -> {
                callbackResult[0] = true;
                callbackResult[1] = arguments[0].getNominalType() == JSType.Null;
                callbackResult[2] = ((JSString) runtime.resolveReference(arguments[1])).getValue().equals(stringToCheck);

                JSReference reference = runtime.newReference(JSType.Integer);
                ((JSInteger) runtime.resolveReference(reference)).setValue(numberToCheck);
                return reference;
            });
        }

        JSReference functionResult = function.invoke(functionRef, arg1, arg2);
        assertTrue(callbackResult[0]);
        assertTrue(callbackResult[1]);
        assertTrue(callbackResult[2]);
        assertEquals(numberToCheck, (long) ((JSInteger) runtime.resolveReference(functionResult)).getValue());

        function.setFunction(arguments -> {
            JSReference reference = runtime.newReference(JSType.Undefined);
            for (int i = 0; i < 1000; i++) {
                reference = runtime.newReference(JSType.Object);
            }
            return reference;
        });
        functionResult = function.invoke(functionRef);
        assertTrue(runtime.resolveReference(functionResult) instanceof JSObject);

        JSReference funRef = runtime.newReference(JSType.Function);
        for (int i = 0; i < 1000; i++) {
            JSFunction fun = runtime.resolveReference(functionRef);
            fun.setFunction(arguments -> {
                callbackResult[0] = false;
                return runtime.newReference(JSType.Null);
            });
            fun.invoke(funRef);
        }
    }
}
