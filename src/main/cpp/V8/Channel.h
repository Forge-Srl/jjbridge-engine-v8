#ifndef CHANNEL_H_
#define CHANNEL_H_

#include <functional>
#include "v8-inspector.h"

class Channel : public v8_inspector::V8Inspector::Channel
{
private:
    std::function<void (const v8_inspector::StringView&)> send;

    void sendResponse(int callId, std::unique_ptr<v8_inspector::StringBuffer> message) override
    {
    	send(message->string());
    }

    void sendNotification(std::unique_ptr<v8_inspector::StringBuffer> message) override
    {
    	send(message->string());
    }

    void flushProtocolNotifications() override {}

public:
	Channel(std::function<void (const v8_inspector::StringView&)> sendCallback)
	{
	    send = std::move(sendCallback);
	}
	~Channel() override = default;
};

#endif