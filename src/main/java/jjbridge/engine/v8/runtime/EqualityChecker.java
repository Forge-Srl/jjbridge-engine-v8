package jjbridge.engine.v8.runtime;

public interface EqualityChecker
{
    boolean checkAreEqual(long firstHandle, long secondHandle);
}
