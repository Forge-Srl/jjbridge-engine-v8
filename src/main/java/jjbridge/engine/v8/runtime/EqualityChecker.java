package jjbridge.engine.v8.runtime;

/**
 * Compares JavaScript objects.
 * */
public interface EqualityChecker
{
    /**
     * Compares two JavaScript objects with the given handles.
     *
     * @param firstHandle the handle of the first JavaScript object
     * @param secondHandle the handle of the second JavaScript object
     * @return {@code true} if the two JavaScript objects are equal, or {@code false} otherwise.
     * */
    boolean checkAreEqual(long firstHandle, long secondHandle);
}
