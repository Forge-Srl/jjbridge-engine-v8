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
	: platform(std::move(v8::platform::NewDefaultPlatform()))
	{}

	virtual void OnCriticalMemoryPressure() override { platform->OnCriticalMemoryPressure(); }
	virtual bool OnCriticalMemoryPressure(size_t length) override { return platform->OnCriticalMemoryPressure(length); }
	virtual int NumberOfWorkerThreads() override { return platform->NumberOfWorkerThreads(); }
	virtual std::shared_ptr<v8::TaskRunner> GetForegroundTaskRunner(v8::Isolate* isolate) override { return platform->GetForegroundTaskRunner(isolate); }
	virtual void CallOnWorkerThread(std::unique_ptr<v8::Task> task) override { platform->CallOnWorkerThread(std::move(task)); }

    // override because default platform is UNIMPLEMENTED when delay_in_seconds != 0
	virtual void CallDelayedOnWorkerThread(std::unique_ptr<v8::Task> task, double delay_in_seconds) override
	{
	    delay_in_seconds = 0;
	    platform->CallDelayedOnWorkerThread(std::move(task), delay_in_seconds);
	}

	virtual bool IdleTasksEnabled(v8::Isolate* isolate) override { return platform->IdleTasksEnabled(isolate); }
	virtual double MonotonicallyIncreasingTime() override { return platform->MonotonicallyIncreasingTime(); }
	virtual double CurrentClockTimeMillis() override { return platform->CurrentClockTimeMillis(); }
	virtual StackTracePrinter GetStackTracePrinter() override { return platform->GetStackTracePrinter(); }
	virtual v8::TracingController* GetTracingController() override { return platform->GetTracingController(); }
	virtual v8::PageAllocator* GetPageAllocator() override { return platform->GetPageAllocator(); }
};

#endif