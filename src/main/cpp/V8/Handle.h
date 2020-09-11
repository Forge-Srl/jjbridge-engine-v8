#ifndef HANDLE_H_
#define HANDLE_H_

#include <jni.h>
#include "v8.h"

class Handle
{
private:
    v8::Isolate* isolate;
    v8::Persistent<v8::Value, v8::CopyablePersistentTraits<v8::Value>>* persistent;

public:
    Handle(v8::Isolate* isolate) : isolate(isolate)
    {
        persistent = new v8::Persistent<v8::Value, v8::CopyablePersistentTraits<v8::Value>>();
    }

    Handle(v8::Isolate* isolate, const v8::Local<v8::Value> &value) : Handle(isolate)
    {
        Set(value);
    }

    inline void Set(const v8::Local<v8::Value> &value)
    {
        persistent->Reset(isolate, value);
    }

    inline void Reset()
    {
        persistent->Reset();
    }

    template<typename V, typename std::enable_if<std::is_base_of<v8::Value, V>::value>::type* = nullptr>
    inline auto GetLocal() -> v8::Local<V>
    {
        return v8::Local<V>::Cast(v8::Local<v8::Value>::New(isolate, *persistent));
    }

    inline auto GetLocal() -> v8::Local<v8::Value>
    {
        return v8::Local<v8::Value>::New(isolate, *persistent);
    }

    template<typename P>
    inline void SetFinalizer(P* parameter, typename v8::WeakCallbackInfo<P>::Callback callback)
    {
        persistent->SetWeak(parameter, callback, v8::WeakCallbackType::kFinalizer);
    }

    template<typename P>
    inline auto GetFinalizerParameter() -> P*
    {
        return persistent->ClearWeak<P>();
    }

    inline static void Release(Handle* handle)
    {
        delete handle->persistent;
        delete handle;
    }

    inline auto AsLong() -> jlong
    {
        return reinterpret_cast<jlong>(this);
    }

    inline static auto FromLong(jlong handle) -> Handle*
    {
        return reinterpret_cast<Handle*>(handle);
    }

    inline auto operator == (const Handle &that) -> bool
    {
        return this->persistent == that.persistent;
    }
};

#endif