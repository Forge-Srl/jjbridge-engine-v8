#include <jni.h>
#include <string>
#include "libplatform/libplatform.h"
#include "v8.h"

#include "V8/Environment.h"
#include "V8/Runtime.h"
#include "V8/Handle.h"
#include "V8/ExternalData.h"
#include "V8/FunctionCallbackData.h"
#include "V8/InspectorClient.h"

#define JPF(methodName) Java_jjbridge_engine_v8_V8_##methodName

Environment* Runtime::environment = nullptr;

extern "C"
{
    JNIEXPORT auto JNICALL
    JNI_OnLoad(JavaVM* vm, void* reserved) -> jint
    {
        JNIEnv* env;
        jint onLoad_err = -1;
        if (vm->GetEnv((void **)&env, JNI_VERSION) != JNI_OK ) { return onLoad_err; }
        if (env == nullptr) { return onLoad_err; }

        Runtime::environment = new Environment(vm, env);

        return JNI_VERSION;
    }

    JNIEXPORT void JNICALL
    JNI_OnUnload(JavaVM* vm, void* reserved)
    {
        JNIEnv* env;
        if (vm->GetEnv((void **)&env, JNI_VERSION) != JNI_OK ) { return; }
        if (env == nullptr) { return; }

        Environment::Release(env, Runtime::environment);
        Runtime::environment = nullptr;
    }

    JNIEXPORT void JNICALL
    JPF(initializeV8)(JNIEnv* env, jobject thiz, jstring nativeLibraryPath)
    {
        const char* pathString = env->GetStringUTFChars(nativeLibraryPath, JNI_FALSE);
        Runtime::environment->InitializeV8(pathString);
        env->ReleaseStringUTFChars(nativeLibraryPath, pathString);
    }

    JNIEXPORT void JNICALL
    JPF(setFlags)(JNIEnv* env, jobject thiz, jstring flags)
    {
        const char* flagString = env->GetStringUTFChars(flags, JNI_FALSE);
        Runtime::environment->SetFlags(flagString);
        env->ReleaseStringUTFChars(flags, flagString);
    }

    JNIEXPORT auto JNICALL
    JPF(createRuntime)(JNIEnv* env, jobject thiz, jobject referenceMonitor, jobject functionCache,
        jobject typeGetterCache, jobject equalityCheckerCache, jobject externalCache) -> jlong
    {
        return (new Runtime(env, referenceMonitor, functionCache, typeGetterCache, equalityCheckerCache, externalCache))->getHandle();
    }

    JNIEXPORT auto JNICALL
    JPF(releaseRuntime)(JNIEnv* env, jobject thiz, jlong runtimeHandle) -> jboolean
    {
        return static_cast<jboolean>(Runtime::safeRelease(env, runtimeHandle));
    }

    JNIEXPORT void JNICALL
    JPF(releaseReference)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime::safeCast(env, runtimeHandle);
        Handle::Release(Handle::FromLong(referenceHandle));
    }

    JNIEXPORT auto JNICALL
    JPF(globalObjectReference)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject typeGetter,
        jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Object> global = context->Global();
        return runtime->NewReference(env, global, typeGetter, equalityChecker);
    }

    JNIEXPORT auto JNICALL
    JPF(executeScript)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jstring fileName, jstring sourceCode,
        jobject typeGetter, jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        v8::Local<v8::String> name = runtime->createV8String(env, fileName);
        v8::Local<v8::String> source = runtime->createV8String(env, sourceCode);

        v8::Local<v8::Script> script;
        if (!runtime->compileScript(env, context, name, source, script)) { return nullptr; }
        v8::Local<v8::Value> result;
        if (!runtime->runScript(env, context, script, result)) { return nullptr; }

        return runtime->NewReference(env, result, typeGetter, equalityChecker);
    }

    JNIEXPORT auto JNICALL
    JPF(getReferenceType)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        return runtime->getReferenceType(env, Handle::FromLong(referenceHandle));
    }

    JNIEXPORT auto JNICALL
    JPF(newValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject type, jobject typeGetter,
        jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        return runtime->NewReference(env, new Handle(runtime->isolate), type, typeGetter, equalityChecker);
    }

    JNIEXPORT auto JNICALL
    JPF(equalsValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong firstReferenceHandle,
        jlong secondReferenceHandle) -> jboolean
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Value> first = Handle::FromLong(firstReferenceHandle)->GetLocal<v8::Value>();
        v8::Local<v8::Value> second = Handle::FromLong(secondReferenceHandle)->GetLocal<v8::Value>();
        return static_cast<jboolean>(first->StrictEquals(second));
    }

    JNIEXPORT void JNICALL
    JPF(initUndefinedValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Undefined(runtime->isolate));
    }

    JNIEXPORT void JNICALL
    JPF(initNullValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Null(runtime->isolate));
    }

    JNIEXPORT auto JNICALL
    JPF(getBooleanValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jboolean
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        return static_cast<jboolean>((Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()
            ->ToBoolean(context->GetIsolate()))->Value());
    }

    JNIEXPORT void JNICALL
    JPF(setBooleanValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jboolean value)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Boolean::New(runtime->isolate, (bool)value));
    }

    JNIEXPORT void JNICALL
    JPF(initBooleanValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Boolean::New(runtime->isolate, false));
    }

    JNIEXPORT auto JNICALL
    JPF(getIntegerValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jint
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        return (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToInt32(context)).ToLocalChecked()->Value();
    }

    JNIEXPORT void JNICALL
    JPF(setIntegerValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jint value)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Int32::New(runtime->isolate, (int)value));
    }

    JNIEXPORT void JNICALL
    JPF(initIntegerValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Int32::New(runtime->isolate, 0));
    }

    JNIEXPORT auto JNICALL
    JPF(getDoubleValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jdouble
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        return (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToNumber(context)).ToLocalChecked()->Value();
    }

    JNIEXPORT void JNICALL
    JPF(setDoubleValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jdouble value)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Number::New(runtime->isolate, (double)value));
    }

    JNIEXPORT void JNICALL
    JPF(initDoubleValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Number::New(runtime->isolate, 0.0));
    }

    JNIEXPORT auto JNICALL
    JPF(getStringValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jstring
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::String::Value unicodeString(runtime->isolate,
            (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToString(context)).ToLocalChecked());
        return env->NewString(*unicodeString, unicodeString.length());
    }

    JNIEXPORT void JNICALL
    JPF(setStringValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jstring value)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(runtime->createV8String(env, value));
    }

    JNIEXPORT void JNICALL
    JPF(initStringValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::String::Empty(runtime->isolate));
    }

    JNIEXPORT auto JNICALL
    JPF(getExternalValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::External> external = Handle::FromLong(referenceHandle)->GetLocal<v8::External>();
        auto* data = (ExternalData*) external->Value();
        if (data == nullptr) {
            return nullptr;
        }
        auto* result = data->externalFromCache(env);

        if (env->ExceptionCheck() == JNI_TRUE) {
            return nullptr;
        }
        return result;
    }

    JNIEXPORT void JNICALL
    JPF(setExternalValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jobject value)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        Handle* handle = Handle::FromLong(referenceHandle);

        auto* oldExternalData = handle->GetFinalizerParameter<ExternalData>();
        if (oldExternalData != nullptr)
        {
            oldExternalData->clearReference(env);
            delete oldExternalData;
        }

        auto* externalData = new ExternalData(runtime, handle);
        externalData->storeInCache(env, value);

        handle->Set(v8::External::New(runtime->isolate, externalData));
        handle->SetFinalizer<ExternalData>(externalData,
            [](const v8::WeakCallbackInfo<ExternalData>& data) {
                ExternalData* externalData = data.GetParameter();
                JNIEnv* env;
                auto attachedThread = Runtime::environment->getCurrentThreadEnv(&env, JNI_VERSION);
                auto* runtime = externalData->runtime;
                newLocalContext(runtime, finalizerContext)
                externalData->clearReference(env);
                delete externalData;
                if (attachedThread == 1)
                {
                    Runtime::environment->releaseCurrentThreadEnv();
                }
            });
    }

    JNIEXPORT void JNICALL
    JPF(initExternalValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        auto* externalData = new ExternalData(runtime, nullptr);
        Handle::FromLong(referenceHandle)->Set(v8::External::New(runtime->isolate, externalData));
    }

    JNIEXPORT auto JNICALL
    JPF(getObjectProperty)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
        jstring property, jobject typeGetter, jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();
        v8::Local<v8::String> propertyName = runtime->createV8String(env, property);

        v8::TryCatch tryCatch(runtime->isolate);
        v8::MaybeLocal<v8::Value> maybePropertyValue = object->Get(context, propertyName);

        if (tryCatch.HasCaught())
        {
            runtime->throwExecutionException(env, context, &tryCatch);
            return nullptr;
        }

        if (maybePropertyValue.IsEmpty())
        {
            runtime->throwExecutionException(env, u"Unable to get property value");
            return nullptr;
        }

        return runtime->NewReference(env, maybePropertyValue.ToLocalChecked(), typeGetter, equalityChecker);
    }

    JNIEXPORT void JNICALL
    JPF(setObjectProperty)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
        jstring property, jlong valueHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();
        v8::Local<v8::String> propertyName = runtime->createV8String(env, property);
        v8::Local<v8::Value> propertyValue = Handle::FromLong(valueHandle)->GetLocal<v8::Value>();

        v8::TryCatch tryCatch(runtime->isolate);
        object->Set(context, propertyName, propertyValue);

        if (tryCatch.HasCaught())
        {
            runtime->throwExecutionException(env, context, &tryCatch);
        }
    }

    JNIEXPORT void JNICALL
    JPF(initObjectValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Object::New(runtime->isolate));
    }

    JNIEXPORT auto JNICALL
    JPF(getDateTimeString)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle) -> jstring
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();

        v8::Function* function = v8::Function::Cast(*(object->Get(context,
            v8::String::NewFromUtf8Literal(runtime->isolate, "toISOString")).ToLocalChecked()));
        v8::Local<v8::Value> dateTime = (function->Call(context, object, 0, nullptr)).ToLocalChecked();
        v8::String::Value unicodeString(runtime->isolate, (dateTime->ToString(context)).ToLocalChecked());
        return env->NewString(*unicodeString, unicodeString.length());
    }

    JNIEXPORT void JNICALL
    JPF(setDateTime)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jstring dateTime)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Object> global = context->Global();

        v8::Function* dateConstructor = v8::Function::Cast(*(global->Get(context,
            v8::String::NewFromUtf8Literal(runtime->isolate, "Date"))).ToLocalChecked());
        const int argc = 1;
        v8::Local<v8::Value> argv[argc] = { runtime->createV8String(env, dateTime) };
        v8::Local<v8::Date> dateObj = v8::Local<v8::Date>::Cast((dateConstructor->NewInstance(context, argc, argv)).ToLocalChecked());

        Handle::FromLong(referenceHandle)->Set(dateObj);
    }

    JNIEXPORT void JNICALL
    JPF(initDateTimeValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Date::New(context, 0.0).ToLocalChecked());
    }

    JNIEXPORT auto JNICALL
    JPF(invokeFunction)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong functionHandle, jlong receiverHandle,
        jlongArray argHandles, jobject typeGetter, jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        jsize argCount = argHandles == nullptr ? 0 : env->GetArrayLength(argHandles);
        auto* arguments = new v8::Local<v8::Value>[argCount];
        if (argCount > 0)
        {
            jlong* argHandleArray = env->GetLongArrayElements(argHandles, JNI_FALSE);
            for (int i = 0; i < argCount; ++i)
            {
                arguments[i] = Handle::FromLong(argHandleArray[i])->GetLocal<v8::Value>();
            }
            env->ReleaseLongArrayElements(argHandles, argHandleArray, 0);
        }

        v8::TryCatch tryCatch(runtime->isolate);
        v8::Local<v8::Function> function = Handle::FromLong(functionHandle)->GetLocal<v8::Function>();
        v8::Local<v8::Value> receiver = Handle::FromLong(receiverHandle)->GetLocal<v8::Value>();
        v8::MaybeLocal<v8::Value> maybeValue = function->Call(context, receiver, argCount, arguments);

        if (tryCatch.HasCaught())
        {
            runtime->throwExecutionException(env, context, &tryCatch);
            return nullptr;
        }

        if (maybeValue.IsEmpty())
        {
            runtime->throwExecutionException(env, u"Empty function result.");
            return nullptr;
        }

        return runtime->NewReference(env, maybeValue.ToLocalChecked(), typeGetter, equalityChecker);
    }

    JNIEXPORT auto JNICALL
    JPF(invokeConstructor)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong functionHandle, jlongArray argHandles,
        jobject typeGetter, jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        jsize argCount = argHandles == nullptr ? 0 : env->GetArrayLength(argHandles);
        auto* arguments = new v8::Local<v8::Value>[argCount];
        if (argCount > 0)
        {
            jlong* argHandleArray = env->GetLongArrayElements(argHandles, JNI_FALSE);
            for (int i = 0; i < argCount; ++i)
            {
                arguments[i] = Handle::FromLong(argHandleArray[i])->GetLocal<v8::Value>();
            }
            env->ReleaseLongArrayElements(argHandles, argHandleArray, 0);
        }

        v8::TryCatch tryCatch(runtime->isolate);
        v8::Local<v8::Function> function = Handle::FromLong(functionHandle)->GetLocal<v8::Function>();
        v8::MaybeLocal<v8::Object> maybeValue = function->NewInstance(context, argCount, arguments);

        if (tryCatch.HasCaught())
        {
            runtime->throwExecutionException(env, context, &tryCatch);
            return nullptr;
        }

        if (maybeValue.IsEmpty())
        {
            runtime->throwExecutionException(env, u"Empty function result.");
            return nullptr;
        }

        return runtime->NewReference(env, maybeValue.ToLocalChecked(), typeGetter, equalityChecker);
    }

    JNIEXPORT void JNICALL
    JPF(setFunctionHandler)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jobject handler,
        jobject typeGetter, jobject equalityChecker)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        Handle* handle = Handle::FromLong(referenceHandle);
        auto* oldCallbackData = handle->GetFinalizerParameter<FunctionCallbackData>();
        if (oldCallbackData != nullptr)
        {
            oldCallbackData->clearReference(env);
            delete oldCallbackData;
        }

        auto* callbackData = new FunctionCallbackData(runtime, handle);
        callbackData->storeInCache(env, handler, typeGetter, equalityChecker);
        v8::Local<v8::External> additionalData = v8::External::New(runtime->isolate, callbackData);

        v8::Local<v8::FunctionTemplate> functionTemplate = v8::FunctionTemplate::New(runtime->isolate,
            [](const v8::FunctionCallbackInfo<v8::Value>& args)
            {
                v8::Local<v8::External> data = v8::Local<v8::External>::Cast(args.Data());
                auto* callbackData = static_cast<FunctionCallbackData*>(data->Value());

                JNIEnv* env;
                auto attachedThread = Runtime::environment->getCurrentThreadEnv(&env, JNI_VERSION);

                Runtime* runtime = callbackData->runtime;
                jobject callback = callbackData->callbackFromCache(env);
                if (env->ExceptionCheck() == JNI_TRUE) {
                    jthrowable exception = env->ExceptionOccurred();
                    env->ExceptionClear();
                    runtime->throwJNIExceptionInJS(env, exception);
                    args.GetReturnValue().SetUndefined();
                    if (attachedThread == 1)
                    {
                        Runtime::environment->releaseCurrentThreadEnv();
                    }
                    return;
                }
                jobject typeGetter = callbackData->typeGetterFromCache(env);
                if (env->ExceptionCheck() == JNI_TRUE) {
                    jthrowable exception = env->ExceptionOccurred();
                    env->ExceptionClear();
                    runtime->throwJNIExceptionInJS(env, exception);
                    args.GetReturnValue().SetUndefined();
                    if (attachedThread == 1)
                    {
                        Runtime::environment->releaseCurrentThreadEnv();
                    }
                    return;
                }
                jobject equalityChecker = callbackData->equalityCheckerFromCache(env);
                if (env->ExceptionCheck() == JNI_TRUE) {
                    jthrowable exception = env->ExceptionOccurred();
                    env->ExceptionClear();
                    runtime->throwJNIExceptionInJS(env, exception);
                    args.GetReturnValue().SetUndefined();
                    if (attachedThread == 1)
                    {
                        Runtime::environment->releaseCurrentThreadEnv();
                    }
                    return;
                }

                jsize argCount = args.Length();
                jobjectArray varArguments = Runtime::environment->NewReferenceArray(env, argCount);
                for (int i = 0; i < argCount; ++i)
                {
                    jobject item = runtime->NewReference(env, args[i], typeGetter, equalityChecker);
                    env->SetObjectArrayElement(varArguments, i, item);
                }

                jvalue arguments[1];
                arguments[0].l = varArguments;

                jobject result = Runtime::environment->applyFunctionCallback(env, callback, arguments);
                if (env->ExceptionCheck() == JNI_TRUE) {
                    jthrowable exception = env->ExceptionOccurred();
                    env->ExceptionClear();
                    runtime->throwJNIExceptionInJS(env, exception);
                    args.GetReturnValue().SetUndefined();
                    if (attachedThread == 1)
                    {
                        Runtime::environment->releaseCurrentThreadEnv();
                    }
                    return;
                }

                jlong resultHandle = Runtime::environment->getReferenceHandle(env, result);
                v8::Local<v8::Value> resultValue = Handle::FromLong(resultHandle)->GetLocal<v8::Value>();

                args.GetReturnValue().Set(resultValue);
                if (attachedThread == 1)
                {
                    Runtime::environment->releaseCurrentThreadEnv();
                }
            }, additionalData);

        handle->Set(functionTemplate->GetFunction(context).ToLocalChecked());
        handle->SetFinalizer<FunctionCallbackData>(callbackData,
            [](const v8::WeakCallbackInfo<FunctionCallbackData>& data) {
                FunctionCallbackData* callbackData = data.GetParameter();
                JNIEnv* env;
                auto attachedThread = Runtime::environment->getCurrentThreadEnv(&env, JNI_VERSION);
                auto* runtime = callbackData->runtime;
                newLocalContext(runtime, finalizerContext)
                callbackData->clearReference(env);
                delete callbackData;
                if (attachedThread == 1)
                {
                    Runtime::environment->releaseCurrentThreadEnv();
                }
            });
    }

    JNIEXPORT void JNICALL
    JPF(initFunctionValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::FunctionTemplate::New(runtime->isolate)->GetFunction(context)
            .ToLocalChecked());
    }

    JNIEXPORT auto JNICALL
    JPF(getArraySize)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jint position) -> jint
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();

        return array->Length();
    }

    JNIEXPORT auto JNICALL
    JPF(getElementByPosition)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jint position,
        jobject typeGetter, jobject equalityChecker) -> jobject
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();
        v8::Local<v8::Value> element = array->Get(context, position).ToLocalChecked();

        return runtime->NewReference(env, element, typeGetter, equalityChecker);
    }

    JNIEXPORT void JNICALL
    JPF(setElementByPosition)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
        jint position, jlong valueHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();
        v8::Local<v8::Value> propertyValue = Handle::FromLong(valueHandle)->GetLocal<v8::Value>();

        array->Set(context, position, propertyValue);
    }

    JNIEXPORT void JNICALL
    JPF(initArrayValue)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)
        Handle::FromLong(referenceHandle)->Set(v8::Array::New(runtime->isolate));
    }

    JNIEXPORT auto JNICALL
    JPF(initInspector)(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject messageHandler) -> jlong
    {
        Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        auto* inspectorClient = new InspectorClient(env, messageHandler, runtime);
        inspectorClient->createContext(context, u"JJBridge-V8 Main Context");
        return inspectorClient->getHandle();
    }

    JNIEXPORT void JNICALL
    JPF(closeInspector)(JNIEnv* env, jobject thiz, jlong inspectorHandle)
    {
        InspectorClient* inspectorClient = InspectorClient::safeCast(env, inspectorHandle);
        InspectorClient::Release(env, inspectorClient);
    }

    JNIEXPORT void JNICALL
    JPF(onInspectorMessage)(JNIEnv* env, jobject thiz, jlong inspectorHandle, jstring message)
    {
        InspectorClient* inspectorClient = InspectorClient::safeCast(env, inspectorHandle);
        inspectorClient->onMessageReceive(env, message);
    }
}
