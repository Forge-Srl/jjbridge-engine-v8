#ifndef BLOCKING_QUEUE_H_
#define BLOCKING_QUEUE_H_

#include <queue>
#include <mutex>
#include <condition_variable>

template <typename T> class BlockingQueue
{
private:
    std::queue<T> queue;
    std::mutex mutex;
    std::condition_variable condvar;

    typedef std::lock_guard<std::mutex> lock;
    typedef std::unique_lock<std::mutex> ulock;

public:
    void push(T const &val)
    {
        std::lock_guard<std::mutex> lock(mutex); // prevents multiple pushes corrupting queue_
        bool wake = queue.empty(); // we may need to wake consumer
        queue.push(val);
        if (wake) condvar.notify_one();
    }

    T pop()
    {
        std::unique_lock<std::mutex> ulock(mutex);

        while (queue.empty())
            condvar.wait(ulock);

        // now queue is non-empty and we still have the lock
        T retval = queue.front();
        queue.pop();
        return retval;
    }
};

#endif