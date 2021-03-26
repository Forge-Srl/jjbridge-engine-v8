#ifndef FUNCTION_CALLBACK_DATA_H_
#define FUNCTION_CALLBACK_DATA_H_

#include <jni.h>
#include "Runtime.h"
#include "Handle.h"

class FunctionCallbackData
{
private:
    bool alreadyCleared;

public:
    Runtime* runtime;
    Handle* handle;

    FunctionCallbackData(Runtime* runtime, Handle* handle)
    : runtime(runtime)
    , handle(handle)
    {
        alreadyCleared = false;
    }

    void clearReference(JNIEnv* env)
    {
        if (alreadyCleared)
        {
            return;
        }

        runtime->FunctionCacheDelete(env, handle->AsLong());
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        runtime->TypeGetterCacheDelete(env, handle->AsLong());
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        runtime->EqualityCheckerCacheDelete(env, handle->AsLong());
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        handle->Reset();

        alreadyCleared = true;
    }

    inline void storeInCache(JNIEnv* env, jobject handler, jobject typeGetter, jobject equalityChecker) const
    {
        if (alreadyCleared)
        {
            return;
        }
        runtime->FunctionCacheStore(env, handle->AsLong(), handler);
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        runtime->TypeGetterCacheStore(env, handle->AsLong(), typeGetter);
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        runtime->EqualityCheckerCacheStore(env, handle->AsLong(), equalityChecker);
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
    }

    inline auto callbackFromCache(JNIEnv* env) const -> jobject
    {
        if (alreadyCleared)
        {
            throw std::runtime_error("FunctionCallbackData already cleared!");
        }
        return runtime->FunctionCacheGet(env, handle->AsLong());
    }

    inline auto typeGetterFromCache(JNIEnv* env) const -> jobject
    {
        if (alreadyCleared)
        {
            throw std::runtime_error("FunctionCallbackData already cleared!");
        }
        return runtime->TypeGetterCacheGet(env, handle->AsLong());
    }

    inline auto equalityCheckerFromCache(JNIEnv* env) const -> jobject
    {
        if (alreadyCleared)
        {
            throw std::runtime_error("FunctionCallbackData already cleared!");
        }
        return runtime->EqualityCheckerCacheGet(env, handle->AsLong());
    }
};

#endif