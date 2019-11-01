#include "InspectorClient.h"

#include <jni.h>

#include "v8-inspector.h"
#include "Environment.h"
#include "Runtime.h"
#include "Channel.h"
#include "BlockingQueue.h"

InspectorClient::InspectorClient(JNIEnv* env, jobject handler, Runtime* runtime)
: runtime(runtime)
, messageHandler(env->NewGlobalRef(handler))
{
    inspector = v8_inspector::V8Inspector::create(runtime->isolate, this);
    channel.reset(new Channel([this](const v8_inspector::StringView& string){
        send(string);
    }));
    session = inspector->connect(contextGroupId, channel.get(), v8_inspector::StringView());
    messageQueue = new BlockingQueue<v8_inspector::StringView>();
}

void InspectorClient::runMessageLoopOnPause(int contextGroupId)
{
    if (runningNestedLoop) return;

    terminated = false;
    runningNestedLoop = true;

    while (!terminated)
    {
        dispatchMessage(messageQueue->pop());
    }

    runningNestedLoop = false;
}

void InspectorClient::quitMessageLoopOnPause()
{
    terminated = true;
}

void InspectorClient::send(const v8_inspector::StringView& string)
{
    v8::Locker locker(runtime->isolate);
    v8::HandleScope handleScope(runtime->isolate);

    v8::MaybeLocal<v8::String> maybeString = string.is8Bit()
        ? v8::String::NewFromOneByte(runtime->isolate, string.characters8(), v8::NewStringType::kNormal, string.length())
        : v8::String::NewFromTwoByte(runtime->isolate, string.characters16(), v8::NewStringType::kNormal, string.length());

    if (maybeString.IsEmpty()) return;

    v8::String::Utf8Value message(runtime->isolate, maybeString.ToLocalChecked());

    JNIEnv* env;
    int result = Runtime::environment->getCurrentThreadEnv(&env, JNI_VERSION);
    Runtime::environment->sendToInspector(env, messageHandler, *message);
    if (result == 1) Runtime::environment->releaseCurrentThreadEnv();
}

void InspectorClient::dispatchMessage(const v8_inspector::StringView& string)
{
    v8::Locker locker(runtime->isolate);
    session->dispatchProtocolMessage(string);
}

void InspectorClient::createContext(const v8::Local<v8::Context> context, const char* name)
{
    v8_inspector::StringView contextName((uint8_t*) name, strlen(name));
    inspector->contextCreated(v8_inspector::V8ContextInfo(context, contextGroupId, contextName));
}

void InspectorClient::onMessageReceive(JNIEnv* env, jstring message)
{
    const uint16_t* unicodeString = env->GetStringChars(message, nullptr);
    int length = env->GetStringLength(message);
    v8_inspector::StringView content(unicodeString, length);
    env->ReleaseStringChars(message, unicodeString);

    if (terminated)
    {
        dispatchMessage(content);
    }
    else
    {
        messageQueue->push(content);
    }
}

InspectorClient* InspectorClient::safeCast(JNIEnv* env, jlong inspectorHandle)
{
   if (inspectorHandle == 0) {
      Runtime::environment->throwNullPointerException(env, "Inspector handle is null.");
   }

  return reinterpret_cast<InspectorClient*>(inspectorHandle);
}

void InspectorClient::Release(JNIEnv* env, InspectorClient* client)
{
    v8::Locker locker(client->runtime->isolate);

    client->quitMessageLoopOnPause();
    client->session->resume();
    client->session.reset(nullptr);
    client->channel.reset(nullptr);
    client->inspector.reset(nullptr);
    env->DeleteGlobalRef(client->messageHandler);
    delete client;
}