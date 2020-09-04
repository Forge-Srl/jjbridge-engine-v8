#ifndef RUNTIME_H_
#define RUNTIME_H_

#include <jni.h>
#include <string>
#include "libplatform/libplatform.h"
#include "v8.h"

#include "Environment.h"
#include "Handle.h"

#define newLocalContext(runtime, contextVar) v8::Isolate::Scope isolateScope(runtime->isolate);\
    v8::Locker locker(runtime->isolate);\
	v8::HandleScope handle_scope(runtime->isolate);\
	v8::Local<v8::Context> contextVar = runtime->context.Get(runtime->isolate);\
	v8::Context::Scope context_scope(contextVar);

class Runtime
{
private:
    const jobject referenceMonitor;
    const jobject functionCache;
    const jobject typeGetterCache;
    const jobject equalityCheckerCache;
    const jobject externalCache;

public:
	static Environment* environment;
	v8::Isolate* isolate;
	v8::Persistent<v8::Context> context;

	Runtime(JNIEnv* env, jobject referenceMonitor, jobject functionCache, jobject typeGetterCache, jobject equalityCheckerCache, jobject externalCache);

    inline jlong getHandle() const
    {
        return reinterpret_cast<jlong>(this);
    }

	v8::Local<v8::String> createV8String(JNIEnv* env, jstring &string) const;

	bool compileScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::String> fileName, v8::Local<v8::String> source, v8::Local<v8::Script> &script);
	bool runScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::Script> script, v8::Local<v8::Value> &result);

	void throwJNIExceptionInJS(JNIEnv* env, jthrowable throwable);
	void throwExecutionException(JNIEnv* env, v8::Local<v8::Context> context, v8::TryCatch* tryCatch);
	void throwExecutionException(JNIEnv* env, std::u16string message);

    inline jobject getReferenceType(JNIEnv* env, Handle* handle)
    {
        return environment->getResultType(env, handle->GetLocal<v8::Value>());
    }

    inline jobject NewReference(JNIEnv* env, const v8::Local<v8::Value>& value, jobject typeGetter, jobject equalityChecker)
    {
        Handle* handle = new Handle(isolate, value);
        return NewReference(env, handle, getReferenceType(env, handle), typeGetter, equalityChecker);
    }

    inline jobject NewReference(JNIEnv* env, Handle* handle, jobject type, jobject typeGetter, jobject equalityChecker) const
	{
	    jobject reference = environment->NewReference(env, handle->AsLong(), type, typeGetter, equalityChecker);
	    environment->trackReference(env, this->getHandle(), reference, this->referenceMonitor);
		return reference;
	}

    inline void FunctionCacheStore(JNIEnv* env, jlong handle, jobject object) const
    {
    	env->CallVoidMethod(functionCache, environment->cacheStore, handle, object);
    }

    inline jobject FunctionCacheGet(JNIEnv* env, jlong handle) const
    {
    	return env->CallObjectMethod(functionCache, environment->cacheGet, handle);
    }

    inline void FunctionCacheDelete(JNIEnv* env, jlong handle) const
    {
    	env->CallVoidMethod(functionCache, environment->cacheDelete, handle);
    }

    inline void TypeGetterCacheStore(JNIEnv* env, jlong handle, jobject object) const
    {
    	env->CallVoidMethod(typeGetterCache, environment->cacheStore, handle, object);
    }

    inline jobject TypeGetterCacheGet(JNIEnv* env, jlong handle) const
    {
    	return env->CallObjectMethod(typeGetterCache, environment->cacheGet, handle);
    }

    inline void TypeGetterCacheDelete(JNIEnv* env, jlong handle) const
    {
    	env->CallVoidMethod(typeGetterCache, environment->cacheDelete, handle);
    }

    inline void EqualityCheckerCacheStore(JNIEnv* env, jlong handle, jobject object) const
    {
    	env->CallVoidMethod(equalityCheckerCache, environment->cacheStore, handle, object);
    }

    inline jobject EqualityCheckerCacheGet(JNIEnv* env, jlong handle) const
    {
    	return env->CallObjectMethod(equalityCheckerCache, environment->cacheGet, handle);
    }

    inline void EqualityCheckerCacheDelete(JNIEnv* env, jlong handle) const
    {
    	env->CallVoidMethod(equalityCheckerCache, environment->cacheDelete, handle);
    }

    inline void ExternalCacheStore(JNIEnv* env, jlong handle, jobject object) const
    {
    	env->CallVoidMethod(externalCache, environment->cacheStore, handle, object);
    }

    inline jobject ExternalCacheGet(JNIEnv* env, jlong handle) const
    {
    	return env->CallObjectMethod(externalCache, environment->cacheGet, handle);
    }

    inline void ExternalCacheDelete(JNIEnv* env, jlong handle) const
    {
    	env->CallVoidMethod(externalCache, environment->cacheDelete, handle);
    }

	static Runtime* safeCast(JNIEnv* env, jlong runtimeHandle);
	static bool safeRelease(JNIEnv* env, jlong runtimeHandle);
};

#endif