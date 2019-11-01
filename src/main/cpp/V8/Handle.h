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
    inline v8::Local<V> GetLocal()
    {
        return v8::Local<V>::Cast(v8::Local<v8::Value>::New(isolate, *persistent));
    }

    template<>
    inline v8::Local<v8::Value> GetLocal<v8::Value>()
    {
        return v8::Local<v8::Value>::New(isolate, *persistent);
    }

    template<typename P>
    inline void SetFinalizer(P* parameter, typename v8::WeakCallbackInfo<P>::Callback callback)
    {
        persistent->SetWeak(parameter, callback, v8::WeakCallbackType::kFinalizer);
    }

    template<typename P>
    inline P* GetFinalizerParameter()
    {
        return persistent->ClearWeak<P>();
    }

    inline static void Release(Handle* handle)
    {
        delete handle->persistent;
        delete handle;
    }

    inline jlong AsLong()
    {
        return reinterpret_cast<jlong>(this);
    }

    inline static Handle* FromLong(jlong handle)
    {
        return reinterpret_cast<Handle*>(handle);
    }

    inline bool operator == (const Handle &that)
    {
        return this->persistent == that.persistent;
    }
};

#endif