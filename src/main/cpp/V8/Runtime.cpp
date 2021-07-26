#include "Runtime.h"

#include <string>
#include "libplatform/libplatform.h"
#include "v8.h"
#include "Environment.h"
#include "Handle.h"

auto getMessage(v8::Local<v8::Context> context, v8::TryCatch* tryCatch) -> std::u16string
{
    v8::MaybeLocal<v8::Value> stack = tryCatch->StackTrace(context);
    v8::Local<v8::String> message;
    if (stack.IsEmpty()) { message = tryCatch->Message()->Get(); }
    else { message = v8::Local<v8::String>::Cast(stack.ToLocalChecked()); }

    v8::String::Value unicodeString(context->GetIsolate(), message);
    std::u16string str((char16_t*) *unicodeString);
    return str;
}

Runtime::Runtime(JNIEnv* env, jobject runtime, jobject functionCache, jobject typeGetterCache, jobject equalityCheckerCache, jobject externalCache)
: runtime(env->NewGlobalRef(runtime))
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

auto Runtime::createV8String(JNIEnv* env, jstring &string) const -> v8::Local<v8::String>
{
	const uint16_t* unicodeString = env->GetStringChars(string, nullptr);
	int length = env->GetStringLength(string);
	v8::Local<v8::String> result = v8::String::NewFromTwoByte(isolate, unicodeString, v8::NewStringType::kNormal, length).ToLocalChecked();
	env->ReleaseStringChars(string, unicodeString);
	return result;
}

auto Runtime::compileScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::String> fileName,
    v8::Local<v8::String> source, v8::Local<v8::Script> &script) const -> bool
{
	v8::TryCatch tryCatch(isolate);

	v8::Local<v8::Integer> originRow = v8::Integer::New(isolate, 0);
    v8::Local<v8::Integer> originCol = v8::Integer::New(isolate, 0);
    v8::Local<v8::Boolean> cors = v8::Boolean::New(isolate, true);
    static int ScriptIdCounter = 0;
    v8::Local<v8::Integer> scriptId = v8::Integer::New(isolate, ScriptIdCounter++);
    v8::Local<v8::String> originUrl = v8::String::Concat(isolate, v8::String::NewFromUtf8Literal(isolate, "file://"), fileName);

	auto* origin = new v8::ScriptOrigin(fileName, originRow, originCol, cors, scriptId, originUrl);
	v8::MaybeLocal<v8::Script> maybeScript = v8::Script::Compile(context, source, origin);

    if (tryCatch.HasCaught())
	{
	    std::u16string tmp = getMessage(context, &tryCatch);
	    environment->throwCompilationException(env, (jchar*) tmp.c_str(), tmp.length());
    	return false;
	}

	if (maybeScript.IsEmpty())
	{
	    std::u16string tmp = u"Empty script result.";
	    environment->throwCompilationException(env, (jchar*) tmp.c_str(), tmp.length());
	    return false;
	}

	script = maybeScript.ToLocalChecked();
	return true;
}

auto Runtime::runScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::Script> script,
    v8::Local<v8::Value> &result) -> bool
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
	    throwExecutionException(env, u"Empty script result.");
	    return false;
	}

	result = maybeResult.ToLocalChecked();
	return true;
}

void Runtime::throwJNIExceptionInJS(JNIEnv* env, jthrowable throwable) const
{
    jclass clazz = env->GetObjectClass(throwable);
    jmethodID getMessage = env->GetMethodID(clazz, "toString", "()Ljava/lang/String;");
    auto* message = (jstring) env->CallObjectMethod(throwable, getMessage);

    v8::Local<v8::String> jsMessage = createV8String(env, message);
    jsMessage = v8::String::Concat(isolate, v8::String::NewFromUtf8Literal(isolate, "java exception in callback ["), jsMessage);
    jsMessage = v8::String::Concat(isolate, jsMessage, v8::String::NewFromUtf8Literal(isolate, "]."));

    v8::Local<v8::Value> exception = v8::Exception::Error(jsMessage);
    v8::Local<v8::Object>::Cast(exception)
        ->Set(v8::Local<v8::Context>::New(isolate, context),
            v8::String::NewFromUtf8Literal(isolate, "_nativeException"),
            v8::External::New(isolate, throwable));
	isolate->ThrowException(exception);
}

void Runtime::throwExecutionException(JNIEnv* env, v8::Local<v8::Context> context, v8::TryCatch* tryCatch) const
{
    std::u16string message = getMessage(context, tryCatch);
    v8::MaybeLocal<v8::Value> inner = v8::Local<v8::Object>::Cast(tryCatch->Exception())
        ->Get(context, v8::String::NewFromUtf8Literal(isolate, "_nativeException"));

    if (inner.IsEmpty())
    {
        environment->throwExecutionException(env, (jchar*) message.c_str(), message.length());
    }
    else
    {
        jthrowable exception = (jthrowable) v8::Local<v8::External>::Cast(inner.ToLocalChecked())->Value();
        environment->throwExecutionException(env, (jchar*) message.c_str(), message.length(), exception);
    }
}

void Runtime::throwExecutionException(JNIEnv* env, const std::u16string &message)
{
    environment->throwExecutionException(env, (jchar*) message.c_str(), message.length());
}

auto Runtime::safeCast(JNIEnv* env, jlong runtimeHandle) -> Runtime*
{
	if (runtimeHandle == 0) {
	    std::u16string error = u"Runtime handle is null.";
		environment->throwNullPointerException(env, (jchar*) error.c_str(), error.length());
	}

	return reinterpret_cast<Runtime*>(runtimeHandle);
}

auto Runtime::safeRelease(JNIEnv* env, jlong runtimeHandle) -> bool
{
	if (runtimeHandle == 0)
	{
	    return true;
	}

	auto* runtime = reinterpret_cast<Runtime*>(runtimeHandle);
	runtime->isolate->LowMemoryNotification(); //Forces garbage collection before dispose
	env->DeleteGlobalRef(runtime->runtime);

	env->CallVoidMethod(runtime->functionCache, environment->cacheClear);
	if (env->ExceptionCheck() == JNI_TRUE) {
        env->ExceptionClear();
    }
	env->DeleteGlobalRef(runtime->functionCache);
	env->CallVoidMethod(runtime->typeGetterCache, environment->cacheClear);
	if (env->ExceptionCheck() == JNI_TRUE) {
        env->ExceptionClear();
    }
	env->DeleteGlobalRef(runtime->typeGetterCache);
	env->CallVoidMethod(runtime->externalCache, environment->cacheClear);
	if (env->ExceptionCheck() == JNI_TRUE) {
        env->ExceptionClear();
    }
	env->DeleteGlobalRef(runtime->externalCache);

	runtime->context.Reset();
	runtime->isolate->Dispose();
	delete runtime;

	return true;
}