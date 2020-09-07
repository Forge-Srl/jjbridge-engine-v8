package jjbridge.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReferenceMonitorTest {
    private ReferenceMonitor<Object> referenceMonitor;

    @Before
    public void before() {
        referenceMonitor = new ReferenceMonitor<>(50);
    }

    @Test
    public void run() {
        referenceMonitor.start();

        boolean[] cleanUpDone = new boolean[100];
        for (int i = 0; i < cleanUpDone.length; i++) {
            cleanUpDone[i] = false;
            int finalI = i;
            referenceMonitor.track(new Object(), () -> cleanUpDone[finalI] = true);
        }

        System.gc();
        loseTime(20000000);
        System.gc();

        for (int i = 0; i < cleanUpDone.length; i++)
        {
            assertTrue("Clean up " + i, cleanUpDone[i]);
        }

        referenceMonitor.interrupt();

        try {
            referenceMonitor.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    private static void loseTime(long n) {
        StringBuffer buff = new StringBuffer();
        for (long i = 0; i < n; i++) {
            buff.append('a');
        }
        String t = buff.toString();
    }
}
