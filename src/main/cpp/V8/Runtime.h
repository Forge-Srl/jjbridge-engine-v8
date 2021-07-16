#ifndef RUNTIME_H_
#define RUNTIME_H_

#include <jni.h>
#include <string>
#include "libplatform/libplatform.h"
#include "v8.h"

#include "Environment.h"
#include "Handle.h"

#define newLocalContext(runtime, contextVar) v8::Isolate::Scope isolateScope((runtime)->isolate);\
    v8::Locker locker((runtime)->isolate);\
	v8::HandleScope handle_scope((runtime)->isolate);\
	v8::Local<v8::Context> (contextVar) = (runtime)->context.Get((runtime)->isolate);\
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

	Runtime(JNIEnv* env, jobject referenceMonitor, jobject functionCache, jobject typeGetterCache,
	    jobject equalityCheckerCache, jobject externalCache);

    inline auto getHandle() const -> jlong
    {
        return reinterpret_cast<jlong>(this);
    }

	auto createV8String(JNIEnv* env, jstring &string) const -> v8::Local<v8::String>;

	auto compileScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::String> fileName,
	    v8::Local<v8::String> source, v8::Local<v8::Script> &script) const -> bool;
	auto runScript(JNIEnv* env, v8::Local<v8::Context> context, v8::Local<v8::Script> script,
	    v8::Local<v8::Value> &result) -> bool;

	void throwJNIExceptionInJS(JNIEnv* env, jthrowable throwable) const;
	void throwExecutionException(JNIEnv* env, v8::Local<v8::Context> context, v8::TryCatch* tryCatch) const;
	void throwExecutionException(JNIEnv* env, const std::u16string &message);

    inline auto getReferenceType(JNIEnv* env, v8::Local<v8::Context> context, Handle* handle) -> jobject
    {
        return environment->getResultType(env, context, handle->GetLocal<v8::Value>());
    }

    inline auto NewReference(JNIEnv* env, v8::Local<v8::Context> context, const v8::Local<v8::Value>& value,
        jobject typeGetter, jobject equalityChecker) -> jobject
    {
        auto* handle = new Handle(isolate, value);
        return NewReference(env, handle, getReferenceType(env, context, handle), typeGetter, equalityChecker);
    }

    inline auto NewReference(JNIEnv* env, Handle* handle, jobject type, jobject typeGetter,
        jobject equalityChecker) const -> jobject
	{
	    jobject reference = environment->NewReference(env, handle->AsLong(), type, typeGetter, equalityChecker);
	    environment->trackReference(env, this->getHandle(), reference, this->referenceMonitor);
	    if (env->ExceptionCheck() == JNI_TRUE) {
        	// Don't clear so there is no need to rethrow exception
        	// env->ExceptionClear();
            return nullptr;
        }
		return reference;
	}

    inline void FunctionCacheStore(JNIEnv* env, jlong handle, jobject object) const
    {
    	env->CallVoidMethod(functionCache, environment->cacheStore, handle, object);
    }

    inline auto FunctionCacheGet(JNIEnv* env, jlong handle) const -> jobject
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

    inline auto TypeGetterCacheGet(JNIEnv* env, jlong handle) const -> jobject
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

    inline auto EqualityCheckerCacheGet(JNIEnv* env, jlong handle) const -> jobject
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

    inline auto ExternalCacheGet(JNIEnv* env, jlong handle) const -> jobject
    {
    	return env->CallObjectMethod(externalCache, environment->cacheGet, handle);
    }

    inline void ExternalCacheDelete(JNIEnv* env, jlong handle) const
    {
    	env->CallVoidMethod(externalCache, environment->cacheDelete, handle);
    }

	static auto safeCast(JNIEnv* env, jlong runtimeHandle) -> Runtime*;
	static auto safeRelease(JNIEnv* env, jlong runtimeHandle) -> bool;
};

#endif