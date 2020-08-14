#ifndef INSPECTOR_CLIENT_H_
#define INSPECTOR_CLIENT_H_

#include <jni.h>

#include "v8-inspector.h"
#include "Environment.h"
#include "Runtime.h"
#include "Channel.h"
#include "BlockingQueue.h"

class InspectorClient : public v8_inspector::V8InspectorClient
{
private:
    static const int contextGroupId = 1;

    const Runtime* runtime;
    const jobject messageHandler;
    std::unique_ptr<v8_inspector::V8Inspector> inspector;
    std::unique_ptr<v8_inspector::V8InspectorSession> session;
    std::unique_ptr<v8_inspector::V8Inspector::Channel> channel;

    BlockingQueue<v8_inspector::StringView>* messageQueue;
    bool terminated = true;
    bool runningNestedLoop = false;

    void runMessageLoopOnPause(int contextGroupId) override;
    void quitMessageLoopOnPause() override;

    void send(const v8_inspector::StringView& string);
    void dispatchMessage(const v8_inspector::StringView& string);

public:
    InspectorClient(JNIEnv* env, jobject handler, Runtime* runtime);

    inline jlong getHandle() const
    {
        return reinterpret_cast<jlong>(this);
    }

    void createContext(const v8::Local<v8::Context> context, const char* name);
    void onMessageReceive(JNIEnv* env, jstring message);

    static InspectorClient* safeCast(JNIEnv* env, jlong inspectorHandle);
    static void Release(JNIEnv* env, InspectorClient* client);
};

#endif