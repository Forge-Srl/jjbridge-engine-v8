package jjbridge.engine.v8.runtime;

import jjbridge.api.value.strategy.ArrayDataGetter;
import jjbridge.api.value.strategy.ArrayDataSetter;
import jjbridge.api.value.strategy.FunctionCallback;
import jjbridge.api.value.strategy.FunctionInvoker;
import jjbridge.api.value.strategy.FunctionSetter;
import jjbridge.api.value.strategy.ObjectPropertyGetter;
import jjbridge.api.value.strategy.ObjectPropertySetter;
import jjbridge.api.value.strategy.ValueGetter;
import jjbridge.api.value.strategy.ValueSetter;
import jjbridge.engine.v8.V8;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

class AccessorsFactory
{
    private final V8 v8;
    private final long runtimeHandle;
    private ReferenceTypeGetter referenceTypeGetter;
    private EqualityChecker equalityChecker;

    protected AccessorsFactory(V8 v8, long runtimeHandle)
    {
        this.v8 = v8;
        this.runtimeHandle = runtimeHandle;
    }

    protected ReferenceTypeGetter referenceTypeGetter()
    {
        if (referenceTypeGetter == null)
        {
            referenceTypeGetter = handle -> this.v8.getReferenceType(this.runtimeHandle, handle);
        }
        return referenceTypeGetter;
    }

    protected EqualityChecker equalityChecker()
    {
        if (equalityChecker == null)
        {
            equalityChecker = (firstHandle, secondHandle) ->
                    this.v8.equalsValue(this.runtimeHandle, firstHandle, secondHandle);
        }
        return equalityChecker;
    }

    protected ValueGetter<Boolean> booleanGetter(long handle)
    {
        return () -> this.v8.getBooleanValue(this.runtimeHandle, handle);
    }

    protected ValueSetter<Boolean> booleanSetter(long handle)
    {
        return value -> this.v8.setBooleanValue(this.runtimeHandle, handle, value);
    }

    protected ValueGetter<Long> longGetter(long handle)
    {
        return () -> this.v8.getLongValue(this.runtimeHandle, handle);
    }

    protected ValueSetter<Long> longSetter(long handle)
    {
        return value -> this.v8.setLongValue(this.runtimeHandle, handle, value);
    }

    protected ValueGetter<Double> doubleGetter(long handle)
    {
        return () -> this.v8.getDoubleValue(this.runtimeHandle, handle);
    }

    protected ValueSetter<Double> doubleSetter(long handle)
    {
        return value -> this.v8.setDoubleValue(this.runtimeHandle, handle, value);
    }

    protected ValueGetter<String> stringGetter(long handle)
    {
        return () -> this.v8.getStringValue(this.runtimeHandle, handle);
    }

    protected ValueSetter<String> stringSetter(long handle)
    {
        return value -> this.v8.setStringValue(this.runtimeHandle, handle, value);
    }

    protected <T> ValueGetter<T> externalGetter(long handle)
    {
        return () -> this.v8.getExternalValue(this.runtimeHandle, handle);
    }

    protected <T> ValueSetter<T> externalSetter(long handle)
    {
        return value -> this.v8.setExternalValue(this.runtimeHandle, handle, value);
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    protected ValueGetter<Date> dateGetter(long handle)
    {
        return () ->
        {
            String dateTimeString = this.v8.getDateTimeString(this.runtimeHandle, handle);
            try
            {
                return new Date(OffsetDateTime.parse(dateTimeString, dateTimeFormatter).toInstant().toEpochMilli());
            }
            catch (DateTimeParseException e)
            {
                throw new IllegalArgumentException("Wrong date-time format.");
            }
        };
    }

    protected ValueSetter<Date> dateSetter(long handle)
    {
        return value ->
        {
            String format = value.toInstant().atOffset(ZoneOffset.UTC).format(dateTimeFormatter);
            this.v8.setDateTimeString(this.runtimeHandle, handle, format);
        };
    }

    protected ObjectPropertyGetter<Reference> propertyGetter(long handle)
    {
        return name -> this.v8.getObjectProperty(this.runtimeHandle, handle, name, referenceTypeGetter(),
                equalityChecker());
    }

    protected ObjectPropertySetter<Reference> propertySetter(long handle)
    {
        return (name, value) -> this.v8.setObjectProperty(this.runtimeHandle, handle, name, value.handle);
    }

    protected ArrayDataGetter<Reference> arrayDataGetter(long handle)
    {
        return new ArrayDataGetter<Reference>()
        {
            @Override
            public int getSize()
            {
                return v8.getArraySize(runtimeHandle, handle);
            }

            @Override
            public Reference getItemByPosition(int position)
            {
                return v8.getElementByPosition(runtimeHandle, handle, position, referenceTypeGetter(),
                        equalityChecker());
            }
        };
    }

    protected ArrayDataSetter<Reference> arrayDataSetter(long handle)
    {
        return (position, value) -> this.v8.setElementByPosition(this.runtimeHandle, handle, position, value.handle);
    }

    protected FunctionInvoker<Reference> functionInvoker(long handle)
    {
        return new FunctionInvoker<Reference>()
        {
            private long[] referenceToHandle(Reference[] args)
            {
                long[] handles = new long[args.length];
                for (int i = 0; i < args.length; i++)
                {
                    handles[i] = args[i].handle;
                }
                return handles;
            }

            @Override
            public Reference invokeFunction(Reference receiver, Reference[] args)
            {
                return v8.invokeFunction(runtimeHandle, handle, receiver.handle, referenceToHandle(args),
                        referenceTypeGetter(), equalityChecker());
            }

            @Override
            public Reference invokeConstructor(Reference[] args)
            {
                return v8.invokeConstructor(runtimeHandle, handle, referenceToHandle(args), referenceTypeGetter(),
                        equalityChecker());
            }
        };
    }

    protected FunctionSetter<Reference> functionSetter(long handle)
    {
        return (FunctionCallback<Reference> callback) -> this.v8.setFunctionHandler(this.runtimeHandle, handle,
                callback, referenceTypeGetter(), equalityChecker());
    }
}
