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
    const jclass cacheClass;

	const jclass nullPointerExceptionClass;
	const jclass compilationExceptionClass;
	const jclass executionExceptionClass;
	const jmethodID executionExceptionCtor;

    const jfieldID referenceHandleField;
    const jclass jsTypeClass;
    const jclass functionCallbackClass;
    const jmethodID functionCallbackApply;

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
    const jobject jsTypeDouble;
    const jobject jsTypeString;
    const jobject jsTypeExternal;
    const jobject jsTypeObject;
    const jobject jsTypeDate;
    const jobject jsTypeFunction;
    const jobject jsTypeArray;

    const jmethodID messageHandlerSendToInspector;

	Environment(JavaVM* jvm, JNIEnv* env);

	int getCurrentThreadEnv(JNIEnv** env, int version)
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

	jobject getResultType(JNIEnv* env, const v8::Local<v8::Value> &result);

    inline jobject NewReference(JNIEnv* env, jlong objectHandle, jobject type, jobject typeGetter, jobject equalityChecker) const
    {
        return env->NewObject(referenceClass, referenceCtor, objectHandle, type, typeGetter, equalityChecker);
    }

	inline jobjectArray NewReferenceArray(JNIEnv* env, jsize size) const
    {
    	return env->NewObjectArray(size, referenceClass, nullptr);
    }

    inline void trackReference(JNIEnv* env, jlong runtimeHandle, jobject reference, jobject referenceMonitor) const
    {
        env->CallStaticVoidMethod(v8Class, v8trackReference, runtimeHandle, reference, referenceMonitor);
    }

	inline jobject applyFunctionCallback(JNIEnv* env, jobject object, const jvalue* args) const
	{
		return env->CallObjectMethodA(object, functionCallbackApply, args);
	}

	inline jlong getReferenceHandle(JNIEnv* env, jobject reference) const
	{
		return env->GetLongField(reference, referenceHandleField);
	}

	inline void throwNullPointerException(JNIEnv* env, const char* message) const
	{
        env->ThrowNew(nullPointerExceptionClass, message);
	}

	inline void throwCompilationException(JNIEnv* env, const char* message) const
	{
        env->ThrowNew(compilationExceptionClass, message);
	}

	inline void throwExecutionException(JNIEnv* env, const char* message) const
	{
        env->Throw((jthrowable) env->NewObject(executionExceptionClass, executionExceptionCtor, env->NewStringUTF(message)));
	}

	inline void throwExecutionException(JNIEnv* env, const char* message, jthrowable exception) const
	{
        env->Throw((jthrowable) env->NewObject(executionExceptionClass, executionExceptionCtor, env->NewStringUTF(message), exception));
	}

    inline void sendToInspector(JNIEnv* env, jobject object, const char* message) const
	{
		env->CallVoidMethod(object, messageHandlerSendToInspector, env->NewStringUTF(message));
	}
};

#endif