package jjbridge.engine;

public class MemoryTimeWaster
{
    public static void waste(long n) {
        StringBuffer buff = new StringBuffer();
        for (long i = 0; i < n; i++) {
            buff.append('a');
        }
        String t = buff.toString();
    }
}
