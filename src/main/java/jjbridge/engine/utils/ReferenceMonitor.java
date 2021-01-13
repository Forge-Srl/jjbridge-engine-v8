package jjbridge.engine.utils;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class simplifies the interaction with the Java garbage collector, allowing a custom finalization action to be
 * performed on an object before it is definitely cleared from the memory.
 * <p>The usage is the following:</p>
 * <pre>{@code
 * // Initialize the monitor
 * ReferenceMonitor<SomeType> monitor = new ReferenceMonitor<>();
 * monitor.start();
 *
 * // Create your object as usual
 * SomeType obj = new SomeType();
 *
 * // Allow the monitor to track `obj` lifecycle, specifying the action to be performed before garbage collection.
 * monitor.track(obj, () -> {
 *     System.out.println("Performing clean up for `obj`");
 * });
 *
 * // Once all references to `obj` are lost, the garbage collector will detect `obj` can be cleared, and the clean up
 * // action will be performed.
 * }</pre>
 * */
public class ReferenceMonitor<T> extends Thread
{
    private static final AtomicInteger threadId = new AtomicInteger();
    private static final AtomicLong idCounter = new AtomicLong(0);
    private final AtomicBoolean interrupted;
    private final ReferenceQueue<T> referenceQueue;
    private final HashMap<Long, NativeReference<T>> references;

    private final long millisPause;

    /**
     * Creates a reference monitor with default settings.
     * <p>This is equivalent to calling {@link #ReferenceMonitor(long)} with {@code millisPause = 50}</p>
     * */
    public ReferenceMonitor()
    {
        this(50);
    }

    /**
     * Creates a reference monitor.
     *
     * @param millisPause the number of milliseconds the reference monitor will pause its execution if there are no
     *                    pending references to clear.
     * */
    public ReferenceMonitor(long millisPause)
    {
        super("Reference Monitor [" + threadId.getAndIncrement() + "]");
        this.millisPause = millisPause;
        this.interrupted = new AtomicBoolean(false);
        this.referenceQueue = new ReferenceQueue<>();
        this.references = new HashMap<>();
        this.setDaemon(true);
    }

    private static long generateId()
    {
        return idCounter.getAndIncrement();
    }

    /**
     * Associate a clean up action to be performed by this monitor when the object is garbage collected.
     * <p>For the reference monitor to perform the clean up, it is important that <strong>the clean up action DOES NOT
     * CONTAIN any reference to the tracked object</strong>. <br> Failing in this, the object will never be garbage
     * collected, hence the reference monitor will never run the clean up action.</p>
     *
     * @param object the object to track
     * @param cleanUpAction the action to be performed
     * */
    public synchronized void track(T object, CleanUpAction cleanUpAction)
    {
        NativeReference<T> ref = new NativeReference<>(generateId(), object, this.referenceQueue, cleanUpAction);
        this.references.put(ref.id, ref);
    }

    protected void clean(NativeReference<T> ref)
    {
        ref.cleanUp();
        this.references.remove(ref.id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run()
    {
        while (!this.interrupted.get())
        {
            try
            {
                NativeReference<T> ref = (NativeReference<T>) this.referenceQueue.remove(millisPause);
                if (ref != null)
                {
                    clean(ref);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        this.references.clear();
    }

    @Override
    public void interrupt()
    {
        this.interrupted.set(true);
    }
}
