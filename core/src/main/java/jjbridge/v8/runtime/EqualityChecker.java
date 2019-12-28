package jjbridge.v8.runtime;

public interface EqualityChecker {
    boolean checkAreEqual(long aHandle, long bHandle);
}
