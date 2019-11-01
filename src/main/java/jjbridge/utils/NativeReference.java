package jjbridge.utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class NativeReference<T> extends PhantomReference<T> {
    private final CleanUpAction cleanUpAction;
    public final long id;

    public NativeReference(long id, T referent, ReferenceQueue<? super T> q, CleanUpAction cleanUpAction) {
        super(referent, q);
        this.id = id;
        this.cleanUpAction = cleanUpAction;
    }

    public void cleanUp() {
        this.cleanUpAction.cleanUp();
    }
}
