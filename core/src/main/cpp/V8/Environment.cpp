#include "Environment.h"

#include <jni.h>
#include <string>

#include "libplatform/libplatform.h"
#include "v8.h"
#include "MyPlatform.h"

#define INIT_CLASS(variable, package)                           variable((jclass)env->NewGlobalRef(env->FindClass(package)))
#define INIT_METHOD(variable, class, name, signature)           variable(env->GetMethodID(class, name, signature))
#define INIT_STATIC_METHOD(variable, class, name, signature)    variable(env->GetStaticMethodID(class, name, signature))
#define INIT_FIELD(variable, class, name, signature)            variable(env->GetFieldID(class, name, signature))
#define INIT_ENUM_VALUE(variable, class, name, type)            variable(env->NewGlobalRef(env->GetStaticObjectField(class, env->GetStaticFieldID(class, name, type))))

Environment::Environment(JavaVM* jvm, JNIEnv* env)
: INIT_CLASS(v8Class, "jjbridge/v8/V8")
, INIT_STATIC_METHOD(v8trackReference, v8Class, "track", "(JLjjbridge/v8/runtime/Reference;Ljjbridge/utils/ReferenceMonitor;)V")

, INIT_CLASS(referenceClass, "jjbridge/v8/runtime/Reference")
, INIT_METHOD(referenceCtor, referenceClass, "<init>", "(JLjjbridge/common/value/JSType;Ljjbridge/v8/runtime/ReferenceTypeGetter;Ljjbridge/v8/runtime/EqualityChecker;)V")
, INIT_FIELD(referenceHandleField, referenceClass, "handle", "J")

, INIT_CLASS(nullPointerExceptionClass, "java/lang/NullPointerException")
, INIT_CLASS(compilationExceptionClass, "jjbridge/common/runtime/CompilationException")
, INIT_CLASS(executionExceptionClass, "jjbridge/common/runtime/ExecutionException")
, INIT_METHOD(executionExceptionCtor, executionExceptionClass, "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V")

, INIT_CLASS(cacheClass, "jjbridge/utils/Cache")
, INIT_METHOD(cacheStore, cacheClass, "store", "(JLjava/lang/Object;)V")
, INIT_METHOD(cacheGet, cacheClass, "get", "(J)Ljava/lang/Object;")
, INIT_METHOD(cacheDelete, cacheClass, "delete", "(J)V")
, INIT_METHOD(cacheClear, cacheClass, "clear", "()V")

, INIT_CLASS(functionCallbackClass, "jjbridge/common/value/strategy/FunctionCallback")
, INIT_METHOD(functionCallbackApply, functionCallbackClass, "apply", "([Ljjbridge/common/runtime/JSReference;)Ljjbridge/common/runtime/JSReference;")

, INIT_CLASS(jsTypeClass, "jjbridge/common/value/JSType")
, INIT_ENUM_VALUE(jsTypeUndefined, jsTypeClass, "Undefined", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeNull, jsTypeClass, "Null", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeBoolean, jsTypeClass, "Boolean", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeInteger, jsTypeClass, "Integer", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeDouble, jsTypeClass, "Double", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeString, jsTypeClass, "String", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeExternal, jsTypeClass, "External", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeObject, jsTypeClass, "Object", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeDate, jsTypeClass, "Date", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeFunction, jsTypeClass, "Function", "Ljjbridge/common/value/JSType;")
, INIT_ENUM_VALUE(jsTypeArray, jsTypeClass, "Array", "Ljjbridge/common/value/JSType;")

, INIT_CLASS(messageHandlerClass, "jjbridge/v8/inspector/V8MessageHandler")
, INIT_METHOD(messageHandlerSendToInspector, messageHandlerClass, "sendToInspector", "(Ljava/lang/String;)V")
{
	_jvm = jvm;

	v8::V8::InitializeICU();
	_platform = new MyPlatform();
	v8::V8::InitializePlatform(_platform);
	v8::V8::Initialize();
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
	env->DeleteGlobalRef(environment->jsTypeDouble);
	env->DeleteGlobalRef(environment->jsTypeString);
	env->DeleteGlobalRef(environment->jsTypeExternal);
	env->DeleteGlobalRef(environment->jsTypeObject);
	env->DeleteGlobalRef(environment->jsTypeDate);
	env->DeleteGlobalRef(environment->jsTypeFunction);
	env->DeleteGlobalRef(environment->jsTypeArray);

	delete environment;
}

jobject Environment::getResultType(JNIEnv* env, const v8::Local<v8::Value> &result)
{
	if (result->IsUndefined()) { return jsTypeUndefined; }
	else if (result->IsNull()) { return jsTypeNull; }
	else if (result->IsBoolean()) { return jsTypeBoolean; }
	else if (result->IsInt32()) { return jsTypeInteger; }
	else if (result->IsNumber()) { return jsTypeDouble;	}
	else if (result->IsString()) { return jsTypeString;	}
	else if (result->IsExternal()) { return jsTypeExternal; }
	else if (result->IsObject())
	{
		if (result->IsArray()) { return jsTypeArray; }
		else if (result->IsDate()) { return jsTypeDate; }
		else if (result->IsFunction()) { return jsTypeFunction; }

		return jsTypeObject;
	}

	return nullptr;
}
