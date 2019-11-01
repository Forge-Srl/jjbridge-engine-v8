package jjbridge.utils;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ReferenceMonitor<T> extends Thread {
    private static final AtomicInteger threadId = new AtomicInteger();
    private static final AtomicLong idCounter = new AtomicLong(0);
    private final AtomicBoolean interrupted;
    private final ReferenceQueue<T> referenceQueue;
    private final HashMap<Long, NativeReference<T>> references;

    private final long millisPause;

    public ReferenceMonitor(long millisPause) {
        super("Reference Monitor [" + threadId.getAndIncrement() + "]");
        this.millisPause = millisPause;
        this.interrupted = new AtomicBoolean(false);
        this.referenceQueue = new ReferenceQueue<>();
        this.references = new HashMap<>();
    }

    private static long generateId() {
        return idCounter.getAndIncrement();
    }

    public synchronized void track(T r, CleanUpAction cleanUpAction) {
        NativeReference<T> ref = new NativeReference<>(generateId(), r, this.referenceQueue, cleanUpAction);
        this.references.put(ref.id, ref);
    }

    protected void clean(NativeReference<T> ref) {
        ref.cleanUp();
        this.references.remove(ref.id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        while (!this.interrupted.get()) {
            try {
                NativeReference<T> ref = (NativeReference<T>) this.referenceQueue.remove(millisPause);
                if (ref != null) { clean(ref); }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.references.clear();
    }

    @Override
    public void interrupt() {
        this.interrupted.set(true);
    }
}
