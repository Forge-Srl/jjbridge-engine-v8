#include "Environment.h"

#include <jni.h>
#include <string>
#include <cmath>

#include "libplatform/libplatform.h"
#include "v8.h"
#include "MyPlatform.h"

#define INIT_CLASS(variable, package)                           variable((jclass)env->NewGlobalRef(env->FindClass(package)))
#define INIT_METHOD(variable, class, name, signature)           variable(env->GetMethodID(class, name, signature))
#define INIT_STATIC_METHOD(variable, class, name, signature)    variable(env->GetStaticMethodID(class, name, signature))
#define INIT_FIELD(variable, class, name, signature)            variable(env->GetFieldID(class, name, signature))
#define INIT_ENUM_VALUE(variable, class, name, type)            variable(env->NewGlobalRef(env->GetStaticObjectField(class, env->GetStaticFieldID(class, name, type))))

Environment::Environment(JavaVM* jvm, JNIEnv* env)
: INIT_CLASS(v8Class, "jjbridge/engine/v8/V8")
, INIT_STATIC_METHOD(v8trackReference, v8Class, "track", "(JLjjbridge/engine/v8/runtime/Reference;Ljjbridge/engine/utils/ReferenceMonitor;)V")

, INIT_CLASS(referenceClass, "jjbridge/engine/v8/runtime/Reference")
, INIT_METHOD(referenceCtor, referenceClass, "<init>", "(JLjjbridge/api/value/JSType;Ljjbridge/engine/v8/runtime/ReferenceTypeGetter;Ljjbridge/engine/v8/runtime/EqualityChecker;)V")
, INIT_FIELD(referenceHandleField, referenceClass, "handle", "J")

, INIT_CLASS(nullPointerExceptionClass, "java/lang/NullPointerException")
, INIT_METHOD(nullPointerExceptionCtor, nullPointerExceptionClass, "<init>", "(Ljava/lang/String;)V")
, INIT_CLASS(compilationExceptionClass, "jjbridge/api/runtime/CompilationException")
, INIT_METHOD(compilationExceptionCtor, compilationExceptionClass, "<init>", "(Ljava/lang/String;)V")
, INIT_CLASS(executionExceptionClass, "jjbridge/api/runtime/ExecutionException")
, INIT_METHOD(executionExceptionCtor, executionExceptionClass, "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V")

, INIT_CLASS(cacheClass, "jjbridge/engine/utils/Cache")
, INIT_CLASS(functionCallbackClass, "jjbridge/api/value/strategy/FunctionCallback")
, INIT_METHOD(functionCallbackApply, functionCallbackClass, "apply", "([Ljjbridge/api/runtime/JSReference;)Ljjbridge/api/runtime/JSReference;")
, INIT_CLASS(jsTypeClass, "jjbridge/api/value/JSType")

, INIT_CLASS(messageHandlerClass, "jjbridge/engine/v8/inspector/V8MessageHandler")

, INIT_METHOD(cacheStore, cacheClass, "store", "(JLjava/lang/Object;)V")
, INIT_METHOD(cacheGet, cacheClass, "get", "(J)Ljava/lang/Object;")
, INIT_METHOD(cacheDelete, cacheClass, "delete", "(J)V")
, INIT_METHOD(cacheClear, cacheClass, "clear", "()V")

, INIT_ENUM_VALUE(jsTypeUndefined, jsTypeClass, "Undefined", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeNull, jsTypeClass, "Null", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeBoolean, jsTypeClass, "Boolean", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeInteger, jsTypeClass, "Integer", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeFloat, jsTypeClass, "Float", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeString, jsTypeClass, "String", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeExternal, jsTypeClass, "External", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeObject, jsTypeClass, "Object", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeDate, jsTypeClass, "Date", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeFunction, jsTypeClass, "Function", "Ljjbridge/api/value/JSType;")
, INIT_ENUM_VALUE(jsTypeArray, jsTypeClass, "Array", "Ljjbridge/api/value/JSType;")

, INIT_METHOD(messageHandlerSendToInspector, messageHandlerClass, "sendToInspector", "(Ljava/lang/String;)V")
{
	_jvm = jvm;
}

auto Environment::InitializeV8(const char* libraryPath) -> bool
{
    if (!v8::V8::InitializeICU(libraryPath))
	{
	    return false;
	}
	_platform = new MyPlatform();
	v8::V8::InitializePlatform(_platform);
	v8::V8::Initialize();
	return true;
}

void Environment::Release(JNIEnv* env, Environment* environment)
{
	v8::V8::Dispose();
	v8::V8::ShutdownPlatform();

	env->DeleteGlobalRef(environment->v8Class);
	env->DeleteGlobalRef(environment->referenceClass);
	env->DeleteGlobalRef(environment->cacheClass);
	env->DeleteGlobalRef(environment->nullPointerExceptionClass);
	env->DeleteGlobalRef(environment->compilationExceptionClass);
	env->DeleteGlobalRef(environment->executionExceptionClass);
	env->DeleteGlobalRef(environment->jsTypeClass);
	env->DeleteGlobalRef(environment->functionCallbackClass);
	env->DeleteGlobalRef(environment->jsTypeUndefined);
	env->DeleteGlobalRef(environment->jsTypeNull);
	env->DeleteGlobalRef(environment->jsTypeBoolean);
	env->DeleteGlobalRef(environment->jsTypeInteger);
	env->DeleteGlobalRef(environment->jsTypeFloat);
	env->DeleteGlobalRef(environment->jsTypeString);
	env->DeleteGlobalRef(environment->jsTypeExternal);
	env->DeleteGlobalRef(environment->jsTypeObject);
	env->DeleteGlobalRef(environment->jsTypeDate);
	env->DeleteGlobalRef(environment->jsTypeFunction);
	env->DeleteGlobalRef(environment->jsTypeArray);

	delete environment;
}

auto Environment::getResultType(JNIEnv* env, v8::Local<v8::Context> context, const v8::Local<v8::Value> &result) const -> jobject
{
	if (result->IsUndefined()) { return jsTypeUndefined; }
	if (result->IsNull()) { return jsTypeNull; }
	if (result->IsBoolean()) { return jsTypeBoolean; }
	if (result->IsNumber())
	{
	    if (fmod(result->NumberValue(context).ToChecked(), 1.0) == 0.0) { return jsTypeInteger; }

	    return jsTypeFloat;
	}
	if (result->IsString()) { return jsTypeString; }
	if (result->IsExternal()) { return jsTypeExternal; }
	if (result->IsObject())
	{
		if (result->IsArray()) { return jsTypeArray; }
		if (result->IsDate()) { return jsTypeDate; }
		if (result->IsFunction()) { return jsTypeFunction; }

		return jsTypeObject;
	}

	return nullptr;
}
