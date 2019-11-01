#include "Runtime.h"

#include <sstream>
#include <string>
#include "libplatform/libplatform.h"
#include "v8.h"
#include "Environment.h"
#include "Handle.h"

const char* nativeExceptionField = "_nativeException";

char* getMessage(v8::Local<v8::Context> context, v8::TryCatch* tryCatch)
{
    v8::MaybeLocal<v8::Value> stack = tryCatch->StackTrace(context);
    v8::Local<v8::String> message;
    if (stack.IsEmpty()) { message = tryCatch->Message()->Get(); }
    else { message = v8::Local<v8::String>::Cast(stack.ToLocalChecked()); }

    v8::String::Utf8Value unicodeString(message);
    return *unicodeString;
}

Runtime::Runtime(JNIEnv* env, jobject referenceMonitor, jobject functionCache, jobject typeGetterCache, jobject equalityCheckerCache, jobject externalCache)
: referenceMonitor(env->NewGlobalRef(referenceMonitor))
, functionCache(env->NewGlobalRef(functionCache))
, typeGetterCache(env->NewGlobalRef(typeGetterCache))
, equalityCheckerCache(env->NewGlobalRef(equalityCheckerCache))
, externalCache(env->NewGlobalRef(externalCache))
{
	v8::Isolate::CreateParams create_params;
	create_params.array_buffer_allocator = v8::ArrayBuffer::Allocator::NewDefaultAllocator();
	isolate = v8::Isolate::New(create_params);

    v8::Locker locker(isolate);
	v8::Isolate::Scope isolate_scope(isolate);
	v8::HandleScope handle_scope(isolate);

	v8::Local<v8::Context> ctx = v8::Context::New(isolate);
	context.Reset(isolate, ctx);
}

v8::Local<v8::String> Runtime::createV8String(JNIEnv* env, jstring &string) const
{
	const uint16_t* unicodeString = env->GetStringChars(string, nullptr);
	int length = env->GetStringLength(string);
	v8::Local<v8::String> result = v8::String::NewFromTwoByte(isolate, unicodeString, v8::String::NewStringType::kNormalString, length);
	env->ReleaseStringChars(string, unicodeString);
	return result;
}

bool Runtime::compileScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::String> fileName, v8::Local<v8::String> source, v8::Local<v8::Script> &script)
{
	v8::TryCatch tryCatch(isolate);

	v8::Local<v8::Integer> originRow = v8::Integer::New(isolate, 0);
    v8::Local<v8::Integer> originCol = v8::Integer::New(isolate, 0);
    v8::Local<v8::Boolean> cors = v8::Boolean::New(isolate, true);
    static int ScriptIdCounter = 0;
    v8::Local<v8::Integer> scriptId = v8::Integer::New(isolate, ScriptIdCounter++);
    v8::Local<v8::String> originUrl = v8::String::Concat(v8::String::NewFromUtf8(isolate, "file://"), fileName);

	v8::ScriptOrigin* origin = new v8::ScriptOrigin(fileName, originRow, originCol, cors, scriptId, originUrl);
	v8::MaybeLocal<v8::Script> maybeScript = v8::Script::Compile(context, source, origin);

    if (tryCatch.HasCaught())
	{
	    environment->throwCompilationException(env, getMessage(context, &tryCatch));
    	return false;
	}

	if (maybeScript.IsEmpty())
	{
	    environment->throwCompilationException(env, "Empty script result.");
	    return false;
	}

	script = maybeScript.ToLocalChecked();
	return true;
}

bool Runtime::runScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::Script> script, v8::Local<v8::Value> &result)
{
	v8::TryCatch tryCatch(isolate);
	v8::MaybeLocal<v8::Value> maybeResult = script->Run(context);

	if (tryCatch.HasCaught())
	{
	    throwExecutionException(env, context, &tryCatch);
    	return false;
	}

	if (maybeResult.IsEmpty())
	{
	    throwExecutionException(env, "Empty script result.");
	    return false;
	}

	result = maybeResult.ToLocalChecked();
	return true;
}

void Runtime::throwJNIExceptionInJS(JNIEnv* env, jthrowable throwable)
{
    jclass clazz = env->GetObjectClass(throwable);
    jmethodID getMessage = env->GetMethodID(clazz, "toString", "()Ljava/lang/String;");
    jstring message = (jstring) env->CallObjectMethod(throwable, getMessage);

    std::stringstream ss;
    const char* nativeMessage = env->GetStringUTFChars(message, nullptr);
    std::string msg(nativeMessage);
    ss << "java exception in callback [" << msg << "].";
    env->ReleaseStringUTFChars(message, nativeMessage);

    v8::Local<v8::Value> exception = v8::Exception::Error(v8::String::NewFromUtf8(isolate, ss.str().c_str()));
    v8::Local<v8::Object>::Cast(exception)
        ->Set(v8::Local<v8::Context>::New(isolate, context),
            v8::String::NewFromUtf8(isolate, nativeExceptionField),
            v8::External::New(isolate, throwable));
	isolate->ThrowException(exception);
}

void Runtime::throwExecutionException(JNIEnv* env, v8::Local<v8::Context> context, v8::TryCatch* tryCatch)
{
    char* message = getMessage(context, tryCatch);
    v8::MaybeLocal<v8::Value> inner = v8::Local<v8::Object>::Cast(tryCatch->Exception())
        ->Get(context, v8::String::NewFromUtf8(isolate, nativeExceptionField));

    if (inner.IsEmpty())
    {
        environment->throwExecutionException(env, message);
    }
    else
    {
        jthrowable exception = (jthrowable) v8::Local<v8::External>::Cast(inner.ToLocalChecked())->Value();
        environment->throwExecutionException(env, message, exception);
    }
}

void Runtime::throwExecutionException(JNIEnv* env, const char* message)
{
    environment->throwExecutionException(env, message);
}

Runtime* Runtime::safeCast(JNIEnv* env, jlong runtimeHandle)
{
	if (runtimeHandle == 0) {
		environment->throwNullPointerException(env, "Runtime handle is null.");
	}

	return reinterpret_cast<Runtime*>(runtimeHandle);
}

bool Runtime::safeRelease(JNIEnv* env, jlong runtimeHandle)
{
	if (runtimeHandle == 0) { return true; }

	Runtime* runtime = reinterpret_cast<Runtime*>(runtimeHandle);
	runtime->isolate->LowMemoryNotification(); //Forces garbage collection before dispose
	env->DeleteGlobalRef(runtime->referenceMonitor);

	env->CallVoidMethod(runtime->functionCache, environment->cacheClear);
	env->DeleteGlobalRef(runtime->functionCache);
	env->CallVoidMethod(runtime->typeGetterCache, environment->cacheClear);
	env->DeleteGlobalRef(runtime->typeGetterCache);
	env->CallVoidMethod(runtime->externalCache, environment->cacheClear);
	env->DeleteGlobalRef(runtime->externalCache);

	runtime->context.Reset();
	runtime->isolate->Dispose();
	delete runtime;

	return true;
}