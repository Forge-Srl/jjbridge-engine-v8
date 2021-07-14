package jjbridge.engine.v8.runtime;

import jjbridge.api.runtime.*;
import jjbridge.api.value.*;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.engine.MemoryTimeWaster;
import jjbridge.engine.utils.Cache;
import jjbridge.engine.utils.CleanUpAction;
import jjbridge.engine.utils.NativeReference;
import jjbridge.engine.utils.ReferenceMonitor;
import jjbridge.engine.v8.V8;
import jjbridge.engine.v8.V8Engine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

public class RuntimeTest {
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private V8Engine engine;
    
    @BeforeEach
    public void before() {
        engine = new V8Engine();
    }

    @Test
    public void tryWithResources() {
        try (JSRuntime runtime = engine.newRuntime()) {
            assertNotNull(runtime);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void multipleCloseNotFail() {
        try {
            JSRuntime runtime = engine.newRuntime();
            runtime.close();
            runtime.close();
            runtime.close();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void multipleRuntimes() {
        try {
            JSRuntime runtime1 = engine.newRuntime();
            JSRuntime runtime2 = engine.newRuntime();
            assertNotNull(runtime1);
            assertNotNull(runtime2);
            assertNotEquals(runtime1, runtime2);
            runtime1.executeScript("undefined;");
            runtime2.executeScript("undefined;");
            runtime1.close();

            try {
                runtime1.executeScript("undefined");
                fail();
            } catch (Exception e) {
                assertEquals(RuntimeException.class, e.getClass());
            }

            runtime2.executeScript("undefined;");
            runtime2.close();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void compilationExceptionScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = null;
            try {
                result = runtime.executeScript("#~asd \\ 8*9");
                fail();
            } catch (CompilationException e) {
                assertEquals("SyntaxError: Invalid or unexpected token", e.getMessage());
            } finally {
                assertNull(result);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void executionExceptionScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = null;
            try {
                result = runtime.executeScript("new NotExistingClass()");
                fail();
            } catch (ExecutionException e) {
                assertEquals("ReferenceError: NotExistingClass is not defined\n\t|JS|    at /script_0:1:1", e.getMessage());
            } finally {
                assertNull(result);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void undefinedResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("undefined");
            assertTrue(runtime.resolveReference(result) instanceof JSUndefined);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void nullResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("null");
            assertTrue(runtime.resolveReference(result) instanceof JSNull);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void booleanResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("true");
            assertEquals(true, ((JSBoolean)runtime.resolveReference(result)).getValue());
            result = runtime.executeScript("false");
            assertEquals(false, ((JSBoolean)runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void integerResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("150");
            assertEquals(150, (int) ((JSInteger)runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void doubleResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("-12.985641");
            assertEquals(-12.985641, ((JSDouble)runtime.resolveReference(result)).getValue(), 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void stringResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            String startingString = "S0me kiͷƊ Őf Ѽœiŗd teχt: #‡\u08ADऴቯ ※ℵ◆⠍⾞ゟㅚ\\n\\r\t \uD83D\uDE28⿕\uD83D\uDCAB";
            String expectedString = "S0me kiͷƊ Őf Ѽœiŗd teχt: #‡\u08ADऴቯ ※ℵ◆⠍⾞ゟㅚ\n\r\t \uD83D\uDE28⿕\uD83D\uDCAB";
            JSReference result = runtime.executeScript("'"+startingString+"'");
            assertEquals(expectedString, ((JSString)runtime.resolveReference(result)).getValue());
            result = runtime.executeScript("\""+startingString+"\"");
            assertEquals(expectedString, ((JSString)runtime.resolveReference(result)).getValue());
            result = runtime.executeScript("`"+startingString+"`");
            assertEquals(expectedString, ((JSString)runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void i18nStringSupport() {
        try (JSRuntime runtime = engine.newRuntime()) {
            // We will use turkish language to test correct i18n support
            // See: https://blog.codinghorror.com/whats-wrong-with-turkey/
            String startingString = "asd ıIiİ asd";
            String expectedString = "ASD IIİİ ASD";

            JSReference result = runtime.executeScript("'"+startingString+"'.toLocaleUpperCase('tr-TR')");
            assertEquals(expectedString, ((JSString)runtime.resolveReference(result)).getValue());
            // Just a double check that everything is ok
            result = runtime.executeScript("'"+startingString+"'.toLocaleUpperCase('tr-TR') === '"+expectedString+"'");
            assertTrue(((JSBoolean)runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void objectResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("let k = { xx : 1 , yy : { zz : null } }; k");
            JSObject<?> object = runtime.resolveReference(result);

            JSReference xx = object.get("xx");
            assertEquals(1, (int) ((JSInteger)runtime.resolveReference(xx)).getValue());

            JSReference yy = object.get("yy");
            JSObject<?> yyObject = runtime.resolveReference(yy);
            JSReference zz = yyObject.get("zz");
            assertTrue(runtime.resolveReference(zz) instanceof JSNull);

            JSReference undef = object.get("qqqqqq");
            assertTrue(runtime.resolveReference(undef) instanceof JSUndefined);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void dateResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("new Date(Date.UTC(6403, 3, 14, 7, 58, 33, 197))");
            Date expected = simpleDateFormat.parse("6403-04-14T07:58:33.197Z");
            assertEquals(expected, ((JSDate<?>) runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void functionResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference function = runtime.executeScript("let i = 0; () => i++ + 1000");
            JSFunction<?> functionResult = runtime.resolveReference(function);

            JSInteger result;
            for (int i = 0; i < 100; i++) {
                result = runtime.resolveReference(functionResult.invoke(function));
                assertEquals(1000 + i, (int) result.getValue());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void functionWithArgumentsResultScript() {
        int i = 6;
        double j = -3.7500009;
        String k = "@@@";

        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference r1 = runtime.newReference(JSType.Integer);
            JSReference r2 = runtime.newReference(JSType.Double);
            JSReference r3 = runtime.newReference(JSType.String);
            ((JSInteger)runtime.resolveReference(r1)).setValue(i);
            ((JSDouble)runtime.resolveReference(r2)).setValue(j);
            ((JSString)runtime.resolveReference(r3)).setValue(k);

            JSReference function = runtime.executeScript("let i = 0; (i,j,k) => (i * 1000 + j * 100) + k");
            JSFunction<?> functionResult = runtime.resolveReference(function);

            JSString result = runtime.resolveReference(functionResult.invoke(function, r1, r2, r3));
            assertEquals((i * 1000 + j * 100) + k, result.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void functionResultScript_Exception() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference function = runtime.executeScript("() => { throw new Error('error message'); }");
            JSFunction<?> functionResult = runtime.resolveReference(function);

            try {
                runtime.resolveReference(functionResult.invoke(function));
                fail();
            } catch (ExecutionException e) {
                assertEquals("Error: error message\n\t|JS|    at Function.<anonymous> (/script_0:1:15)", e.getMessage());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void functionClassConstructorScript() {
        String exportedClass = "exportedClass";
        String s = "class A {constructor(x) {this.value = x} static k() {return 2}}; exportedClass = A;";

        try (JSRuntime runtime = engine.newRuntime()) {
            runtime.globalObject().set(exportedClass, runtime.newReference(JSType.Object));
            runtime.executeScript(s);
            JSReference function = runtime.globalObject().get(exportedClass);
            JSFunction<?> functionResult = runtime.resolveReference(function);

            JSFunction<?> k = runtime.resolveReference(functionResult.get("k"));
            assertEquals(2, (int) ((JSInteger)runtime.resolveReference(k.invoke(function))).getValue());

            JSReference reference = runtime.newReference(JSType.Integer);
            ((JSInteger) runtime.resolveReference(reference)).setValue(1000);
            JSObject<?> a = runtime.resolveReference(functionResult.invokeConstructor(reference));
            assertEquals(1000, (int) ((JSInteger)runtime.resolveReference(a.get("value"))).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void arrayResultScript() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference array = runtime.executeScript("[103, 'qwerty', null, [], true]");
            JSArray<?> arrayResult = runtime.resolveReference(array);

            assertEquals(5, arrayResult.size());

            JSValue result = runtime.resolveReference(arrayResult.get(0));
            assertEquals(103, (int) ((JSInteger) result).getValue());
            result = runtime.resolveReference(arrayResult.get(1));
            assertEquals("qwerty", ((JSString) result).getValue());
            result = runtime.resolveReference(arrayResult.get(2));
            assertTrue(result instanceof JSNull);
            assertEquals(JSType.Array, arrayResult.get(3).getActualType());
            result = runtime.resolveReference(arrayResult.get(4));
            assertEquals(true, ((JSBoolean) result).getValue());

            result = runtime.resolveReference(arrayResult.get(137));
            assertTrue(result instanceof JSUndefined);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewUndefinedReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Undefined);
            assertTrue(runtime.resolveReference(result) instanceof JSUndefined);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewNullReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Null);
            assertTrue(runtime.resolveReference(result) instanceof JSNull);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewBooleanReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Boolean);
            JSBoolean booleanResult = runtime.resolveReference(result);
            assertEquals(false, booleanResult.getValue());

            booleanResult.setValue(true);
            assertEquals(true, booleanResult.getValue());
            booleanResult.setValue(false);
            assertEquals(false, booleanResult.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewIntegerReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Integer);
            JSInteger integerResult = runtime.resolveReference(result);
            assertEquals(0, (int) integerResult.getValue());

            integerResult.setValue(603);
            assertEquals(603, (int) integerResult.getValue());
            integerResult.setValue(-945301);
            assertEquals(-945301, (int) integerResult.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewDoubleReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Double);
            JSDouble doubleResult = runtime.resolveReference(result);
            assertEquals(0, doubleResult.getValue(), 0);

            doubleResult.setValue(3.9000000321);
            assertEquals(3.9000000321, doubleResult.getValue(), 0);
            doubleResult.setValue(-100000000.999999001);
            assertEquals(-100000000.999999001, doubleResult.getValue(), 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewDoubleReference_ActuallyInteger() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Double);
            JSDouble doubleResult = runtime.resolveReference(result);

            doubleResult.setValue(3456.0);
            assertEquals(3456, (int) ((JSInteger) runtime.resolveReference(result, TypeResolution.Actual)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewStringReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.String);
            JSString stringResult = runtime.resolveReference(result);
            assertEquals("", stringResult.getValue());

            stringResult.setValue("This is just a _§Tr1ng#");
            assertEquals("This is just a _§Tr1ng#", stringResult.getValue());
            stringResult.setValue("");
            assertEquals("", stringResult.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewExternalReference() {
        HashMap<String, int[]> obj = new HashMap<>();
        obj.put("asd", new int[] {1, 2});
        obj.put("qwerty", new int[] {});
        obj.put("otherKey", new int[] {654654, -89, 0, 3, 654, 789, 0, 1111111});

        HashMap<String, int[]> obj2 = new HashMap<>();

        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.External);
            JSExternal<HashMap<String, int[]>> external = runtime.resolveReference(result);
            assertNull(external.getValue());

            external.setValue(obj);
            assertEquals(obj, external.getValue());
            external.setValue(obj2);
            assertEquals(obj2, external.getValue());
            external.setValue(null);
            assertNull(external.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewDateReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Date);
            JSDate<?> dateResult = runtime.resolveReference(result);
            assertEquals(simpleDateFormat.parse("1970-01-01T00:00:00.000Z"), dateResult.getValue());

            Date expectedDate = simpleDateFormat.parse("103-11-27T14:00:59.901Z");
            dateResult.setValue(expectedDate);
            assertEquals(expectedDate, dateResult.getValue());
            expectedDate = simpleDateFormat.parse("2019-01-10T15:33:30.666Z");
            dateResult.setValue(expectedDate);
            assertEquals(expectedDate, dateResult.getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewObjectReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Object);
            JSObject<?> object = runtime.resolveReference(result);

            JSReference property = object.get("propertyName");
            assertTrue(runtime.resolveReference(property) instanceof JSUndefined);

            {
                JSReference bool = runtime.newReference(JSType.Boolean);
                ((JSBoolean) runtime.resolveReference(bool)).setValue(true);
                object.set("propertyName", bool);
            }

            property = object.get("propertyName");
            JSValue jsValue = runtime.resolveReference(property);
            assertEquals(true, ((JSBoolean) jsValue).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void objectGetter_exception() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("const A = class { get value() {throw new Error('jsError')} }; new A()");
            JSObject<?> obj = runtime.resolveReference(result);
            assertThrows(ExecutionException.class, () -> obj.get("value"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void objectSetter_exception() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("const A = class { set value(x) {throw new Error('jsError')} }; new A()");
            JSObject<?> obj = runtime.resolveReference(result);

            JSReference newReference = runtime.newReference(JSType.Null);
            assertThrows(ExecutionException.class, () -> obj.set("value", newReference));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewArrayReference() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.newReference(JSType.Array);
            JSArray<?> object = runtime.resolveReference(result);

            assertEquals(0, object.size());
            JSReference property = object.get(0);
            assertTrue(runtime.resolveReference(property) instanceof JSUndefined);

            {
                JSReference bool = runtime.newReference(JSType.Boolean);
                ((JSBoolean) runtime.resolveReference(bool)).setValue(true);
                object.set(0, bool);
            }

            assertEquals(1, object.size());
            property = object.get(0);
            assertEquals(true, ((JSBoolean)runtime.resolveReference(property)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void referencesToSameHandleAreEqual() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference item = runtime.newReference(JSType.Integer);
            ((JSInteger)runtime.resolveReference(item)).setValue(150);

            JSReference arrayRef = runtime.newReference(JSType.Array);
            JSArray<?> array = runtime.resolveReference(arrayRef);
            array.set(15, item);
            assertEquals(item, array.get(15));

            JSReference objRef = runtime.newReference(JSType.Object);
            JSObject<?> obj = runtime.resolveReference(objRef);
            obj.set("item", item);
            assertEquals(item, obj.get("item"));

            JSReference scriptRef = runtime.executeScript("x => x");
            JSFunction<?> script = runtime.resolveReference(scriptRef);
            assertEquals(item, script.invoke(scriptRef, item));

            JSReference funcRef = runtime.newReference(JSType.Function);
            JSFunction<?> func = runtime.resolveReference(funcRef);
            func.setFunction(references -> references[0]);
            assertEquals(item, func.invoke(funcRef, item));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewFunctionReference() {
        final String stringToCheck = "some string to check";
        final int numberToCheck = 9856214;

        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference arg1 = runtime.newReference(JSType.Null);
            JSReference arg2 = runtime.newReference(JSType.String);
            JSString arg2String = runtime.resolveReference(arg2);
            arg2String.setValue(stringToCheck);

            JSReference functionRef = runtime.newReference(JSType.Function);
            JSFunction<?> function = runtime.resolveReference(functionRef);

            JSReference functionResult = function.invoke(functionRef);
            assertTrue(runtime.resolveReference(functionResult) instanceof JSFunction);

            boolean[] callbackResult = { false, false, false };
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

            functionResult = function.invoke(functionRef, arg1, arg2);
            assertTrue(callbackResult[0]);
            assertTrue(callbackResult[1]);
            assertTrue(callbackResult[2]);
            assertEquals(numberToCheck, (int) ((JSInteger)runtime.resolveReference(functionResult)).getValue());

            functionResult = function.invoke(functionRef, arg1, arg2);
            assertTrue(callbackResult[0]);
            assertTrue(callbackResult[1]);
            assertTrue(callbackResult[2]);
            assertEquals(numberToCheck, (int) ((JSInteger)runtime.resolveReference(functionResult)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createNewFunctionReference_Exception() {
        RuntimeException innerException = new RuntimeException("Error message with some gibberish text \uD83D\uDE28⿕\uD83D\uDCAB to test Unicode support");

        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference functionRef = runtime.newReference(JSType.Function);
            JSFunction<?> function = runtime.resolveReference(functionRef);

            JSReference functionResult = function.invoke(functionRef);
            assertTrue(runtime.resolveReference(functionResult) instanceof JSFunction);

            {
                function.setFunction(arguments -> { throw innerException; });
            }

            try {
                function.invoke(functionRef);
                fail();
            } catch (ExecutionException e) {
                assertEquals("Error: java exception in callback [" + innerException.toString() + "].", e.getMessage());
            }

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void nestedCalls() {
        String script1 = "(fun, x) => fun(1) + x";
        String script2 = "(x) => x + 1";
        String script3 = "(fun, x) => fun(x*50)";

        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference func1 = runtime.executeScript("script1", script1);
            assertEquals(func1.getActualType(), JSType.Function);

            JSReference funcX = runtime.newReference(JSType.Function);
            ((JSFunction<?>) runtime.resolveReference(funcX)).setFunction(jsReferences -> {
                JSInteger value = runtime.resolveReference(jsReferences[0]);
                JSReference result = runtime.newReference(JSType.Integer);
                ((JSInteger) runtime.resolveReference(result)).setValue(value.getValue() + 1000);

                JSReference func2 = runtime.executeScript("script2", script2);
                assertEquals(func2.getActualType(), JSType.Function);

                JSReference func3 = runtime.executeScript("script3", script3);
                assertEquals(func3.getActualType(), JSType.Function);

                return ((JSFunction<?>) runtime.resolveReference(func3)).invoke(func3, func2, result);
            });
            assertEquals(funcX.getActualType(), JSType.Function);

            JSReference number = runtime.newReference(JSType.Integer);
            ((JSInteger) runtime.resolveReference(number)).setValue(66);

            JSReference result = ((JSFunction<?>) runtime.resolveReference(func1)).invoke(func1, funcX, number);
            assertEquals(50117, (int) ((JSInteger) runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void executeScriptKeepsContextBetweenCalls() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("const variableName = 5;");
            assertTrue(runtime.resolveReference(result) instanceof JSUndefined);
            result = runtime.executeScript("let other = variableName + 10");
            assertTrue(runtime.resolveReference(result) instanceof JSUndefined);
            result = runtime.executeScript("`other: ${other}`");
            assertEquals("other: 15", ((JSString) runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void referenceIsPersistent() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference result = runtime.executeScript("5");
            assertEquals(5, (int) ((JSInteger)runtime.resolveReference(result)).getValue());
            assertEquals(5, (int) ((JSInteger)runtime.resolveReference(result)).getValue());

            JSReference result2 = runtime.executeScript("10");
            assertEquals(10, (int) ((JSInteger)runtime.resolveReference(result2)).getValue());

            assertEquals(5, (int) ((JSInteger)runtime.resolveReference(result)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void referenceIsAccessibleFromDifferentThreads() {
        try (JSRuntime runtime = engine.newRuntime()) {
            JSReference[] result = new JSReference[2];
            java.lang.String[] actualValue = new java.lang.String[2];
            java.lang.String expectedResult1 = "some value";

            // 1. Create some references in a specific thread (even different from the test one)
            Thread t = new Thread(() -> {
                result[0] = runtime.newReference(JSType.String);
                ((JSString)runtime.resolveReference(result[0])).setValue(expectedResult1);

                result[1] = runtime.newReference(JSType.Integer);
            });
            t.start();
            t.join();


            // 2. Perform multiple read and write accesses to the same references from many different threads
            for (int i = 0; i < 10; i++) {
                int randomValue = (int) (Math.random() * 10000);
                actualValue[0] = null;
                actualValue[1] = null;

                JSInteger integerResult = runtime.resolveReference(result[1]);
                Thread p = new Thread(() -> {
                    actualValue[0] = ((JSString) runtime.resolveReference(result[0])).getValue();

                    integerResult.setValue(randomValue);
                    actualValue[1] = integerResult.getValue().toString();
                });
                p.start();
                p.join();

                assertEquals(expectedResult1, actualValue[0]);
                assertEquals(randomValue+"", actualValue[1]);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void functionInDifferentThread() {
        try (JSRuntime runtime = engine.newRuntime())
        {
            JSReference threadFunc = runtime.newReference(JSType.Function);
            Integer[] results = new Integer[5];

            {
                Thread thread = new Thread(() -> {
                    JSReference innerFunc = runtime.executeScript("(x) => x + 2");
                    JSFunction<?> jsFunction = runtime.resolveReference(threadFunc);
                    jsFunction.setFunction(jsReferences -> {
                        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                        for (int i = 0; i < 5; i++)
                        {
                            int finalI = i;
                            service.schedule(() -> {
                                JSFunction<?> f = runtime.resolveReference(innerFunc);
                                JSReference input = runtime.newReference(JSType.Integer);
                                ((JSInteger) runtime.resolveReference(input)).setValue(finalI);
                                JSReference output = f.invoke(innerFunc, input);
                                results[finalI] = ((JSInteger) runtime.resolveReference(output)).getValue();
                            }, i + 1, TimeUnit.SECONDS);
                        }

                        return runtime.newReference(JSType.Undefined);
                    });
                });
                thread.start();
                thread.join();
            }

            JSReference outerFunc = runtime.executeScript("(func) => func()");
            ((JSFunction<?>)runtime.resolveReference(outerFunc)).invoke(outerFunc, threadFunc);
            Thread.sleep(6500);

            assertArrayEquals(results, new Integer[] {2,3,4,5,6});
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void globalObject() {
        String myPropertyName = "myProperty";
        int myPropertyValue = 20197;
        int k = 156;

        try (JSRuntime runtime = engine.newRuntime()) {
            JSObject<?> global = runtime.globalObject();
            JSReference myPropertyRef = runtime.newReference(JSType.Integer);
            ((JSInteger) runtime.resolveReference(myPropertyRef)).setValue(myPropertyValue);
            global.set(myPropertyName, myPropertyRef);

            JSReference result = runtime.executeScript("function x(k) { return "+myPropertyName+" + k; } x("+k+")");
            assertEquals(myPropertyValue + k, (int) ((JSInteger)runtime.resolveReference(result)).getValue());

            runtime.executeScript("function set(k) { "+myPropertyName+" = k; } set("+k+")");
            myPropertyRef = global.get(myPropertyName);
            assertEquals(k, (int) ((JSInteger)runtime.resolveReference(myPropertyRef)).getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void referencesAreGarbageCollected() {
        V8 v8 = spy(V8.class);
        ReferenceMonitorForTest referenceMonitor = new ReferenceMonitorForTest();
        long runtimeHandle = v8.createRuntime(referenceMonitor, new Cache<FunctionCallback<Reference>>(),
                new Cache<ReferenceTypeGetter>(), new Cache<EqualityChecker>(), new Cache<>());

        boolean[] garbageCollected = new boolean[100];

        int[] counter = {0};
        referenceMonitor.additionalAction = () -> {
            garbageCollected[counter[0]] = true;
            counter[0] += 1;
        };

        try(JSRuntime runtime = new Runtime(v8, runtimeHandle, referenceMonitor)) {
            for (int i = 0; i < garbageCollected.length; i++) {
                JSReference ref = runtime.newReference(JSType.Integer);
                ((JSInteger)runtime.resolveReference(ref)).setValue(i);
            }

            MemoryTimeWaster.waste(1000000);
            System.gc();
            Thread.sleep(1500);

            for (boolean b : garbageCollected) {
                assertTrue(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    protected static class ReferenceMonitorForTest extends ReferenceMonitor<Reference> {
        public ReferenceMonitorForTest() {
            super(50);
        }

        public CleanUpAction additionalAction;

        @Override
        protected void clean(NativeReference<Reference> ref) {
            if (additionalAction != null) {
                additionalAction.cleanUp();
            }
            super.clean(ref);
        }
    }
}
