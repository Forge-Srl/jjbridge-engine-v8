package jjbridge.engine.v8.runtime;

import jjbridge.api.value.JSType;
import jjbridge.api.value.strategy.*;
import jjbridge.engine.v8.V8;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessorsFactoryTest {
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Mock private V8 v8;
    private static final long runtimeHandle = 120;
    private static final long referenceHandle = 5;
    private AccessorsFactory factory;

    @BeforeEach
    public void before() {
        factory = new AccessorsFactory(v8, runtimeHandle);
    }

    @Test
    public void referenceTypeGetter() {
        ReferenceTypeGetter getter = factory.referenceTypeGetter();

        JSType jsType = JSType.Null;
        when(v8.getReferenceType(runtimeHandle, referenceHandle)).thenReturn(jsType);
        assertEquals(jsType, getter.getType(referenceHandle));

        //Assertion for singleton
        assertEquals(getter, factory.referenceTypeGetter());
    }

    @Test
    public void booleanGetter() {
        ValueGetter<Boolean> getter = factory.booleanGetter(referenceHandle);
        when(v8.getBooleanValue(runtimeHandle, referenceHandle)).thenReturn(true);
        assertTrue(getter.getValue());
    }

    @Test
    public void booleanSetter() {
        boolean value = true;
        ValueSetter<Boolean> setter = factory.booleanSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setBooleanValue(runtimeHandle, referenceHandle, value);
    }

    @Test
    public void integerGetter() {
        int value = 150;
        ValueGetter<Integer> getter = factory.integerGetter(referenceHandle);
        when(v8.getIntegerValue(runtimeHandle, referenceHandle)).thenReturn(value);
        assertEquals(value, (int) getter.getValue());
    }

    @Test
    public void integerSetter() {
        int value = 150;
        ValueSetter<Integer> setter = factory.integerSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setIntegerValue(runtimeHandle, referenceHandle, value);
    }

    @Test
    public void doubleGetter() {
        double value = 1.061651654654;
        ValueGetter<Double> getter = factory.doubleGetter(referenceHandle);
        when(v8.getDoubleValue(runtimeHandle, referenceHandle)).thenReturn(value);
        assertEquals(value, getter.getValue(),0);
    }

    @Test
    public void doubleSetter() {
        double value = -79813.1654321654002;
        ValueSetter<Double> setter = factory.doubleSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setDoubleValue(runtimeHandle, referenceHandle, value);
    }

    @Test
    public void stringGetter() {
        String value = "qwe#qwe qiwei u";
        ValueGetter<String> getter = factory.stringGetter(referenceHandle);
        when(v8.getStringValue(runtimeHandle, referenceHandle)).thenReturn(value);
        assertEquals(value, getter.getValue());
    }

    @Test
    public void stringSetter() {
        String value = "asdasd asd as asd";
        ValueSetter<String> setter = factory.stringSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setStringValue(runtimeHandle, referenceHandle, value);
    }

    @Test
    public void externalGetter() {
        Object value = new Object();
        ValueGetter<Object> getter = factory.externalGetter(referenceHandle);
        when(v8.getExternalValue(runtimeHandle, referenceHandle)).thenReturn(value);
        assertEquals(value, getter.getValue());
    }

    @Test
    public void externalSetter() {
        Object value = new Object();
        ValueSetter<Object> setter = factory.externalSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setExternalValue(runtimeHandle, referenceHandle, value);
    }

    @Test
    public void dateGetter() throws ParseException {
        Date value = simpleDateFormat.parse("6403-04-14T05:58:33.197Z");
        ValueGetter<Date> getter = factory.dateGetter(referenceHandle);
        when(v8.getDateTimeString(runtimeHandle, referenceHandle)).thenReturn("6403-04-14T05:58:33.197Z");
        assertEquals(value, getter.getValue());
    }

    @Test
    public void dateSetter() throws ParseException {
        Date value = simpleDateFormat.parse("6403-04-14T05:58:33.197Z");
        ValueSetter<Date> setter = factory.dateSetter(referenceHandle);
        setter.setValue(value);
        verify(v8).setDateTime(runtimeHandle, referenceHandle, "6403-04-14T07:58:33.197+02:00");
    }

    @Test
    public void propertyGetter() {
        Reference value = new Reference(referenceHandle, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker());
        String propertyName = "propertyName";
        ObjectPropertyGetter<Reference> getter = factory.propertyGetter(referenceHandle);
        when(v8.getObjectProperty(runtimeHandle, referenceHandle, propertyName, factory.referenceTypeGetter(), factory.equalityChecker())).thenReturn(value);
        when(v8.equalsValue(runtimeHandle, referenceHandle, referenceHandle)).thenReturn(true);
        assertEquals(value, getter.getPropertyByName(propertyName));
    }

    @Test
    public void propertySetter() {
        Reference value = new Reference(referenceHandle, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker());
        String propertyName = "propertyName";

        ObjectPropertySetter<Reference> setter = factory.propertySetter(referenceHandle);
        setter.setPropertyByName(propertyName, value);
        verify(v8).setObjectProperty(runtimeHandle, referenceHandle, propertyName, value.handle);
    }

    @Test
    public void arrayDataGetter() {
        int expected = 100;
        int position = 5;

        Reference value = new Reference(referenceHandle, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker());
        ArrayDataGetter<Reference> getter = factory.arrayDataGetter(referenceHandle);
        when(v8.getArraySize(runtimeHandle, referenceHandle)).thenReturn(expected);
        assertEquals(expected, getter.getSize());
        when(v8.getElementByPosition(runtimeHandle, referenceHandle, position, factory.referenceTypeGetter(), factory.equalityChecker())).thenReturn(value);
        when(v8.equalsValue(runtimeHandle, referenceHandle, referenceHandle)).thenReturn(true);
        assertEquals(value, getter.getItemByPosition(position));
    }

    @Test
    public void arrayDataSetter() {
        int position = 60;
        Reference value = new Reference(referenceHandle, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker());

        ArrayDataSetter<Reference> setter = factory.arrayDataSetter(referenceHandle);
        setter.setItemByPosition(position, value);
        verify(v8).setElementByPosition(runtimeHandle, referenceHandle, position, value.handle);
    }

    @Test
    public void functionInvoker() {
        Reference[] args = {
            new Reference(1, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker()),
            new Reference(265, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker()),
            new Reference(10000000, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker()),
        };

        long[] argHandles = new long[args.length];
        for (int i = 0; i < args.length; i++) {
            argHandles[i] = args[i].handle;
        }

        Reference receiver = new Reference(referenceHandle, JSType.Null, factory.referenceTypeGetter(), factory.equalityChecker());
        FunctionInvoker<Reference> getter = factory.functionInvoker(referenceHandle);
        when(v8.invokeFunction(runtimeHandle, referenceHandle, receiver.handle, argHandles, factory.referenceTypeGetter(), factory.equalityChecker())).thenReturn(null);
        assertNull(getter.invokeFunction(receiver, args));
        when(v8.invokeConstructor(runtimeHandle, referenceHandle, argHandles, factory.referenceTypeGetter(), factory.equalityChecker())).thenReturn(null);
        assertNull(getter.invokeConstructor(args));
    }

    @Test
    public void functionSetter() {
        FunctionCallback<Reference> callback = arguments -> null;

        FunctionSetter<Reference> setter = factory.functionSetter(referenceHandle);
        setter.setFunction(callback);
        verify(v8).setFunctionHandler(runtimeHandle, referenceHandle, callback, factory.referenceTypeGetter(), factory.equalityChecker());
    }
}
