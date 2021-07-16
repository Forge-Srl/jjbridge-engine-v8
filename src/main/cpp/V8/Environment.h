#ifndef ENVIRONMENT_H_
#define ENVIRONMENT_H_

#include <cstring>
#include <jni.h>
#include "libplatform/libplatform.h"
#include "v8.h"

#define JNI_VERSION JNI_VERSION_1_6

class Environment
{
private:
	JavaVM* _jvm;
	v8::Platform* _platform;

    const jclass v8Class;
    const jmethodID v8trackReference;

    const jclass referenceClass;
    const jmethodID referenceCtor;
    const jfieldID referenceHandleField;

	const jclass nullPointerExceptionClass;
	const jmethodID nullPointerExceptionCtor;
	const jclass compilationExceptionClass;
	const jmethodID compilationExceptionCtor;
	const jclass executionExceptionClass;
	const jmethodID executionExceptionCtor;

    const jclass cacheClass;
    const jclass functionCallbackClass;
    const jmethodID functionCallbackApply;
    const jclass jsTypeClass;

    const jclass messageHandlerClass;

public:
    const jmethodID cacheStore;
    const jmethodID cacheGet;
    const jmethodID cacheDelete;
    const jmethodID cacheClear;

    const jobject jsTypeUndefined;
    const jobject jsTypeNull;
    const jobject jsTypeBoolean;
    const jobject jsTypeInteger;
    const jobject jsTypeFloat;
    const jobject jsTypeString;
    const jobject jsTypeExternal;
    const jobject jsTypeObject;
    const jobject jsTypeDate;
    const jobject jsTypeFunction;
    const jobject jsTypeArray;

    const jmethodID messageHandlerSendToInspector;

	Environment(JavaVM* jvm, JNIEnv* env);

	void InitializeV8(const char* libraryPath);

	auto getCurrentThreadEnv(JNIEnv** env, int version) -> int
	{
	    int getEnvStat = _jvm->GetEnv((void**) env, version);

        if (getEnvStat == JNI_EVERSION) {
            return -1;
        }

        if (getEnvStat == JNI_EDETACHED) {
#ifdef __ANDROID__
            if (_jvm->AttachCurrentThread(env, nullptr) != 0) {
#else
            if (_jvm->AttachCurrentThread((void**) env, nullptr) != 0) {
#endif
                return -1;
            }
            return 1;
        }

        return 0;
	}

	void releaseCurrentThreadEnv()
	{
	    _jvm->DetachCurrentThread();
	}

	static void Release(JNIEnv* env, Environment* environment);

    inline static void SetFlags(const char* flags)
	{
    	v8::V8::SetFlagsFromString(flags, strlen(flags));
	}

	auto getResultType(JNIEnv* env, v8::Local<v8::Context> context, const v8::Local<v8::Value> &result) const -> jobject;

    inline auto NewReference(JNIEnv* env, jlong objectHandle, jobject type, jobject typeGetter, jobject equalityChecker) const -> jobject
    {
        return env->NewObject(referenceClass, referenceCtor, objectHandle, type, typeGetter, equalityChecker);
    }

	inline auto NewReferenceArray(JNIEnv* env, jsize size) const -> jobjectArray
    {
    	return env->NewObjectArray(size, referenceClass, nullptr);
    }

    inline void trackReference(JNIEnv* env, jlong runtimeHandle, jobject reference, jobject referenceMonitor) const
    {
        env->CallStaticVoidMethod(v8Class, v8trackReference, runtimeHandle, reference, referenceMonitor);
    }

	inline auto applyFunctionCallback(JNIEnv* env, jobject object, const jvalue* args) const -> jobject
	{
		return env->CallObjectMethodA(object, functionCallbackApply, args);
	}

	inline auto getReferenceHandle(JNIEnv* env, jobject reference) const -> jlong
	{
		return env->GetLongField(reference, referenceHandleField);
	}

	inline void throwNullPointerException(JNIEnv* env, const jchar* message, jsize length) const
	{
        env->Throw((jthrowable) env->NewObject(nullPointerExceptionClass, nullPointerExceptionCtor, env->NewString(message, length)));
	}

	inline void throwCompilationException(JNIEnv* env, const jchar* message, jsize length) const
	{
        env->Throw((jthrowable) env->NewObject(compilationExceptionClass, compilationExceptionCtor, env->NewString(message, length)));
	}

	inline void throwExecutionException(JNIEnv* env, const jchar* message, jsize length) const
	{
        env->Throw((jthrowable) env->NewObject(executionExceptionClass, executionExceptionCtor, env->NewString(message, length)));
	}

	inline void throwExecutionException(JNIEnv* env, const jchar* message, jsize length, jthrowable exception) const
	{
        env->Throw((jthrowable) env->NewObject(executionExceptionClass, executionExceptionCtor, env->NewString(message, length), exception));
	}

    inline void sendToInspector(JNIEnv* env, jobject object, const jchar* message, jsize length) const
	{
		env->CallVoidMethod(object, messageHandlerSendToInspector, env->NewString(message, length));
	}
};

#endif