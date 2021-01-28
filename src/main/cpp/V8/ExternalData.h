#ifndef EXTERNAL_DATA_H_
#define EXTERNAL_DATA_H_

#include <jni.h>
#include "Runtime.h"
#include "Handle.h"

class ExternalData
{
private:
    bool alreadyCleared;

public:
    Runtime* runtime;
    Handle* handle;

    ExternalData(Runtime* runtime, Handle* handle)
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
        runtime->ExternalCacheDelete(env, handle->AsLong());
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
        handle->Reset();

        alreadyCleared = true;
    }

    inline void storeInCache(JNIEnv* env, jobject external) const
    {
        if (alreadyCleared)
        {
            return;
        }
        runtime->ExternalCacheStore(env, handle->AsLong(), external);
        if (env->ExceptionCheck() == JNI_TRUE) {
            return;
        }
    }

    inline auto externalFromCache(JNIEnv* env) const -> jobject
    {
        if (alreadyCleared)
        {
            throw std::runtime_error("ExternalData already cleared!");
        }
        return runtime->ExternalCacheGet(env, handle->AsLong());
    }
};

#endif