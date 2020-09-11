#ifndef MYPLATFORM_H_
#define MYPLATFORM_H_

#include "libplatform/libplatform.h"
#include "v8.h"

class MyPlatform : public v8::Platform
{
private:
	std::shared_ptr<v8::Platform> platform;

public:
	MyPlatform()
	: platform(v8::platform::NewDefaultPlatform())
	{}

    void OnCriticalMemoryPressure() override
    {
        platform->OnCriticalMemoryPressure();
    }

	auto OnCriticalMemoryPressure(size_t length) -> bool override
	{
	    return platform->OnCriticalMemoryPressure(length);
	}

	auto NumberOfWorkerThreads() -> int override
	{
	    return platform->NumberOfWorkerThreads();
	}

	auto GetForegroundTaskRunner(v8::Isolate* isolate) -> std::shared_ptr<v8::TaskRunner> override
	{
	    return platform->GetForegroundTaskRunner(isolate);
	}

	void CallOnWorkerThread(std::unique_ptr<v8::Task> task) override
	{
	    platform->CallOnWorkerThread(std::move(task));
	}

    // override because default platform is UNIMPLEMENTED when delay_in_seconds != 0
	void CallDelayedOnWorkerThread(std::unique_ptr<v8::Task> task, double delay_in_seconds) override
	{
	    delay_in_seconds = 0;
	    platform->CallDelayedOnWorkerThread(std::move(task), delay_in_seconds);
	}

	auto IdleTasksEnabled(v8::Isolate* isolate) -> bool override
	{
	    return platform->IdleTasksEnabled(isolate);
	}

	auto MonotonicallyIncreasingTime() -> double override
	{
	    return platform->MonotonicallyIncreasingTime();
	}

	auto CurrentClockTimeMillis() -> double override
	{
	    return platform->CurrentClockTimeMillis();
	}

	auto GetStackTracePrinter() -> StackTracePrinter override
	{
	    return platform->GetStackTracePrinter();
	}

	auto GetTracingController() -> v8::TracingController* override
	{
	    return platform->GetTracingController();
	}

	auto GetPageAllocator() -> v8::PageAllocator* override
	{
	    return platform->GetPageAllocator();
	}
};

#endif