#include <jni.h>
#include "libplatform/libplatform.h"
#include "v8.h"

#include "V8/Environment.h"
#include "V8/Runtime.h"
#include "V8/Handle.h"
#include "V8/InspectorClient.h"

Environment* Runtime::environment = nullptr;

class FunctionCallbackData
{
private:
    bool alreadyCleared;

public:
	JNIEnv* env;
	Runtime* runtime;
	Handle* handle;

    FunctionCallbackData(JNIEnv* env, Runtime* runtime, Handle* handle)
	: env(env)
	, runtime(runtime)
	, handle(handle)
	{
	    alreadyCleared = false;
	}

	void clearReference()
	{
	    if (alreadyCleared) return;
	    runtime->FunctionCacheDelete(env, handle->AsLong());
        runtime->TypeGetterCacheDelete(env, handle->AsLong());
        runtime->EqualityCheckerCacheDelete(env, handle->AsLong());

        alreadyCleared = true;
	}

	inline void storeInCache(jobject handler, jobject typeGetter, jobject equalityChecker)
	{
	    if (alreadyCleared) return;
	    runtime->FunctionCacheStore(env, handle->AsLong(), handler);
        runtime->TypeGetterCacheStore(env, handle->AsLong(), typeGetter);
        runtime->EqualityCheckerCacheStore(env, handle->AsLong(), equalityChecker);
	}

    inline jobject callbackFromCache()
    {
        if (alreadyCleared) throw std::runtime_error("FunctionCallbackData already cleared!");
        return runtime->FunctionCacheGet(env, handle->AsLong());
    }

    inline jobject typeGetterFromCache()
    {
        if (alreadyCleared) throw std::runtime_error("FunctionCallbackData already cleared!");
        return runtime->TypeGetterCacheGet(env, handle->AsLong());
    }

    inline jobject equalityCheckerFromCache()
    {
        if (alreadyCleared) throw std::runtime_error("FunctionCallbackData already cleared!");
        return runtime->EqualityCheckerCacheGet(env, handle->AsLong());
    }
};

class ExternalData
{
private:
    bool alreadyCleared;

public:
	JNIEnv* env;
	Runtime* runtime;
	Handle* handle;

    ExternalData(JNIEnv* env, Runtime* runtime, Handle* handle)
	: env(env)
	, runtime(runtime)
	, handle(handle)
	{
	    alreadyCleared = false;
	}

	void clearReference()
	{
	    if (alreadyCleared) return;
	    runtime->ExternalCacheDelete(env, handle->AsLong());
	    handle->Reset();

        alreadyCleared = true;
	}

	inline void storeInCache(jobject external)
	{
	    if (alreadyCleared) return;
	    runtime->ExternalCacheStore(env, handle->AsLong(), external);
	}

    inline jobject externalFromCache()
    {
        if (alreadyCleared) throw std::runtime_error("ExternalData already cleared!");
        return runtime->ExternalCacheGet(env, handle->AsLong());
    }
};

extern "C"
{
	JNIEXPORT jint JNICALL
	JNI_OnLoad(JavaVM* vm, void* reserved)
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
	Java_jjbridge_v8_V8_setFlags(JNIEnv* env, jobject thiz, jstring flags)
	{
	    const char* flagString = env->GetStringUTFChars(flags, 0);
        Runtime::environment->SetFlags(flagString);
        env->ReleaseStringUTFChars(flags, flagString);
	}

	JNIEXPORT jlong JNICALL
	Java_jjbridge_v8_V8_createRuntime(JNIEnv* env, jobject thiz, jobject referenceMonitor, jobject functionCache,
	    jobject typeGetterCache, jobject equalityCheckerCache, jobject externalCache)
	{
		return (new Runtime(env, referenceMonitor, functionCache, typeGetterCache, equalityCheckerCache, externalCache))->getHandle();
	}

	JNIEXPORT jboolean JNICALL
	Java_jjbridge_v8_V8_releaseRuntime(JNIEnv* env, jobject thiz, jlong runtimeHandle)
	{
		return Runtime::safeRelease(env, runtimeHandle);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_releaseReference(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		Handle::Release(Handle::FromLong(referenceHandle));
	}

	JNIEXPORT jobject JNICALL
    Java_jjbridge_v8_V8_globalObjectReference(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject typeGetter,
        jobject equalityChecker)
    {
    	Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
    	newLocalContext(runtime, context)
    	v8::Local<v8::Object> global = context->Global();
    	return runtime->NewReference(env, global, typeGetter, equalityChecker);
    }

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_executeScript(JNIEnv* env, jobject thiz, jlong runtimeHandle, jstring fileName, jstring sourceCode,
	    jobject typeGetter, jobject equalityChecker)
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

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_getReferenceType(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		return runtime->getReferenceType(env, Handle::FromLong(referenceHandle));
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_newValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject type, jobject typeGetter,
	    jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		return runtime->NewReference(env, new Handle(runtime->isolate), type, typeGetter, equalityChecker);
	}

    JNIEXPORT jboolean JNICALL
	Java_jjbridge_v8_V8_equalsValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong aReferenceHandle, jlong bReferenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		return Handle::FromLong(aReferenceHandle) == Handle::FromLong(aReferenceHandle);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initUndefinedValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Undefined(runtime->isolate));
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initNullValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Null(runtime->isolate));
	}

	JNIEXPORT jboolean JNICALL
	Java_jjbridge_v8_V8_getBooleanValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		return (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToBoolean(context->GetIsolate()))->Value();
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setBooleanValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jboolean value)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Boolean::New(runtime->isolate, (bool)value));
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initBooleanValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Boolean::New(runtime->isolate, false));
	}

	JNIEXPORT jint JNICALL
	Java_jjbridge_v8_V8_getIntegerValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		return (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToInt32(context)).ToLocalChecked()->Value();
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setIntegerValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jint value)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Int32::New(runtime->isolate, (int)value));
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initIntegerValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Int32::New(runtime->isolate, 0));
	}

	JNIEXPORT jdouble JNICALL
	Java_jjbridge_v8_V8_getDoubleValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		return (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToNumber(context)).ToLocalChecked()->Value();
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setDoubleValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jdouble value)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Number::New(runtime->isolate, (double)value));
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initDoubleValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Number::New(runtime->isolate, 0.0));
	}

	JNIEXPORT jstring JNICALL
	Java_jjbridge_v8_V8_getStringValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::String::Utf8Value unicodeString(runtime->isolate, (Handle::FromLong(referenceHandle)->GetLocal<v8::Value>()->ToString(context)).ToLocalChecked());
		return env->NewStringUTF(*unicodeString);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setStringValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jstring value)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(runtime->createV8String(env, value));
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initStringValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::String::NewFromUtf8(runtime->isolate, "").ToLocalChecked());
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_getExternalValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::External> external = Handle::FromLong(referenceHandle)->GetLocal<v8::External>();
		ExternalData* data = (ExternalData*) external->Value();
		return data == nullptr ? nullptr : data->externalFromCache();
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setExternalValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jobject value)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)

        Handle* handle = Handle::FromLong(referenceHandle);

        ExternalData* oldExternalData = handle->GetFinalizerParameter<ExternalData>();
        if (oldExternalData != nullptr)
        {
            oldExternalData->clearReference();
            delete oldExternalData;
        }

		ExternalData* externalData = new ExternalData(env, runtime, handle);
        externalData->storeInCache(value);

		handle->Set(v8::External::New(runtime->isolate, externalData));
		handle->SetFinalizer<ExternalData>(externalData,
		    [](const v8::WeakCallbackInfo<ExternalData>& data) {
		        ExternalData* externalData = data.GetParameter();
		        externalData->clearReference();
		        delete externalData;
		    });
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initExternalValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		ExternalData* externalData = new ExternalData(env, runtime, nullptr);
		Handle::FromLong(referenceHandle)->Set(v8::External::New(runtime->isolate, externalData));
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_getObjectProperty(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
	    jstring property, jobject typeGetter, jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();
		v8::Local<v8::String> propertyName = runtime->createV8String(env, property);
		v8::Local<v8::Value> propertyValue = object->Get(context, propertyName).ToLocalChecked();

		return runtime->NewReference(env, propertyValue, typeGetter, equalityChecker);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setObjectProperty(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
	    jstring property, jlong valueHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();
		v8::Local<v8::String> propertyName = runtime->createV8String(env, property);
		v8::Local<v8::Value> propertyValue = Handle::FromLong(valueHandle)->GetLocal<v8::Value>();

		object->Set(context, propertyName, propertyValue);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initObjectValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Object::New(runtime->isolate));
	}

	JNIEXPORT jstring JNICALL
	Java_jjbridge_v8_V8_getDateTimeString(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Object> object = Handle::FromLong(referenceHandle)->GetLocal<v8::Object>();

		v8::Function* function = v8::Function::Cast(*(object->Get(context, v8::String::NewFromUtf8(runtime->isolate, "toISOString").ToLocalChecked()).ToLocalChecked()));
		v8::Local<v8::Value> dateTime = (function->Call(context, object, 0, nullptr)).ToLocalChecked();
		v8::String::Utf8Value unicodeString(runtime->isolate, (dateTime->ToString(context)).ToLocalChecked());
		return env->NewStringUTF(*unicodeString);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setDateTime(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jstring dateTime)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Object> global = context->Global();

		v8::Function* dateConstructor = v8::Function::Cast(*(global->Get(context, v8::String::NewFromUtf8(runtime->isolate, "Date").ToLocalChecked())).ToLocalChecked());
		const int argc = 1;
		v8::Local<v8::Value> argv[argc] = { runtime->createV8String(env, dateTime) };
		v8::Local<v8::Date> dateObj = v8::Local<v8::Date>::Cast((dateConstructor->NewInstance(context, argc, argv)).ToLocalChecked());

		Handle::FromLong(referenceHandle)->Set(dateObj);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initDateTimeValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Date::New(context, 0.0).ToLocalChecked());
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_invokeFunction(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong functionHandle,
	    jlong receiverHandle, jlongArray argHandles, jobject typeGetter, jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)

		jsize argCount = argHandles == nullptr ? 0 : env->GetArrayLength(argHandles);
		v8::Local<v8::Value> arguments[argCount];
        if (argCount > 0)
		{
			jlong* argHandleArray = env->GetLongArrayElements(argHandles, 0);
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
        	runtime->throwExecutionException(env, "Empty function result.");
            return nullptr;
        }

		return runtime->NewReference(env, maybeValue.ToLocalChecked(), typeGetter, equalityChecker);
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_invokeConstructor(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong functionHandle,
	    jlongArray argHandles, jobject typeGetter, jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)

		jsize argCount = argHandles == nullptr ? 0 : env->GetArrayLength(argHandles);
		v8::Local<v8::Value> arguments[argCount];
        if (argCount > 0)
		{
			jlong* argHandleArray = env->GetLongArrayElements(argHandles, 0);
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
        	runtime->throwExecutionException(env, "Empty function result.");
            return nullptr;
        }

		return runtime->NewReference(env, maybeValue.ToLocalChecked(), typeGetter, equalityChecker);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setFunctionHandler(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
	    jobject handler, jobject typeGetter, jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)

        Handle* handle = Handle::FromLong(referenceHandle);
        FunctionCallbackData* oldCallbackData = handle->GetFinalizerParameter<FunctionCallbackData>();
        if (oldCallbackData != nullptr)
        {
            oldCallbackData->clearReference();
            delete oldCallbackData;
        }

        FunctionCallbackData* callbackData = new FunctionCallbackData(env, runtime, handle);
        callbackData->storeInCache(handler, typeGetter, equalityChecker);
		v8::Local<v8::External> additionalData = v8::External::New(runtime->isolate, callbackData);

		v8::Local<v8::FunctionTemplate> functionTemplate = v8::FunctionTemplate::New(runtime->isolate,
			[](const v8::FunctionCallbackInfo<v8::Value>& args)
			{
				v8::Local<v8::External> data = v8::Local<v8::External>::Cast(args.Data());
				FunctionCallbackData* callbackData = static_cast<FunctionCallbackData*>(data->Value());

				JNIEnv* env = callbackData->env;
				Runtime* runtime = callbackData->runtime;
                jobject callback = callbackData->callbackFromCache();
                jobject typeGetter = callbackData->typeGetterFromCache();
                jobject equalityChecker = callbackData->equalityCheckerFromCache();

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
				if (env->ExceptionCheck()) {
				    jthrowable exception = env->ExceptionOccurred();
				    env->ExceptionClear();
                    runtime->throwJNIExceptionInJS(env, exception);
                    args.GetReturnValue().SetUndefined();
                    return;
                }

				jlong resultHandle = Runtime::environment->getReferenceHandle(env, result);
				v8::Local<v8::Value> resultValue = Handle::FromLong(resultHandle)->GetLocal<v8::Value>();

				args.GetReturnValue().Set(resultValue);
			}, additionalData);

		handle->Set(functionTemplate->GetFunction(context).ToLocalChecked());
		handle->SetFinalizer<FunctionCallbackData>(callbackData,
		    [](const v8::WeakCallbackInfo<FunctionCallbackData>& data) {
		        FunctionCallbackData* callbackData = data.GetParameter();
		        callbackData->clearReference();
                delete callbackData;
		    });
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initFunctionValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::FunctionTemplate::New(runtime->isolate)->GetFunction(context).ToLocalChecked());
	}

	JNIEXPORT jint JNICALL
	Java_jjbridge_v8_V8_getArraySize(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle, jint position)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();

		return array->Length();
	}

	JNIEXPORT jobject JNICALL
	Java_jjbridge_v8_V8_getElementByPosition(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
	    jint position, jobject typeGetter, jobject equalityChecker)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();
		v8::Local<v8::Value> element = array->Get(context, position).ToLocalChecked();

		return runtime->NewReference(env, element, typeGetter, equalityChecker);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_setElementByPosition(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle,
	    jint position, jlong valueHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		v8::Local<v8::Array> array = Handle::FromLong(referenceHandle)->GetLocal<v8::Array>();
		v8::Local<v8::Value> propertyValue = Handle::FromLong(valueHandle)->GetLocal<v8::Value>();

		array->Set(context, position, propertyValue);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_initArrayValue(JNIEnv* env, jobject thiz, jlong runtimeHandle, jlong referenceHandle)
	{
		Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
		newLocalContext(runtime, context)
		Handle::FromLong(referenceHandle)->Set(v8::Array::New(runtime->isolate));
	}

	JNIEXPORT jlong JNICALL
	Java_jjbridge_v8_V8_initInspector(JNIEnv* env, jobject thiz, jlong runtimeHandle, jobject messageHandler)
	{
	    Runtime* runtime = Runtime::safeCast(env, runtimeHandle);
        newLocalContext(runtime, context)

        InspectorClient* inspectorClient = new InspectorClient(env, messageHandler, runtime);
        inspectorClient->createContext(context, "JJBridge-V8 Main Context");
        return inspectorClient->getHandle();
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_closeInspector(JNIEnv* env, jobject thiz, jlong inspectorHandle)
	{
	    InspectorClient* inspectorClient = InspectorClient::safeCast(env, inspectorHandle);
	    InspectorClient::Release(env, inspectorClient);
	}

	JNIEXPORT void JNICALL
	Java_jjbridge_v8_V8_onInspectorMessage(JNIEnv* env, jobject thiz, jlong inspectorHandle, jstring message)
	{
	    InspectorClient* inspectorClient = InspectorClient::safeCast(env, inspectorHandle);
        inspectorClient->onMessageReceive(env, message);
	}
}