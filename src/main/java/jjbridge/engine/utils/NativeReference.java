package jjbridge.engine.utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * This class associates a {@link CleanUpAction} to a {@link PhantomReference}.
 * <p>This allows to perform additional clean up operation before the reference is cleared by the garbage collector.</p>
 *
 * @see ReferenceMonitor
 * */
public class NativeReference<T> extends PhantomReference<T>
{
    private final CleanUpAction cleanUpAction;
    public final long id;

    NativeReference(long id, T referent, ReferenceQueue<? super T> q, CleanUpAction cleanUpAction)
    {
        super(referent, q);
        this.id = id;
        this.cleanUpAction = cleanUpAction;
    }

    /**
     * Performs the {@link CleanUpAction} passed in
     * {@link #NativeReference(long, Object, ReferenceQueue, CleanUpAction)}.
     * */
    public void cleanUp()
    {
        this.cleanUpAction.cleanUp();
    }
}
