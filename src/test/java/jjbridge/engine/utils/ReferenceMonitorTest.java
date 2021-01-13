package jjbridge.engine.utils;

import jjbridge.engine.MemoryTimeWaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ReferenceMonitorTest {
    private ReferenceMonitor<Object> referenceMonitor;

    @BeforeEach
    public void before() {
        referenceMonitor = new ReferenceMonitor<>(50);
    }

    @Test
    public void isDaemon() {
        assertTrue(referenceMonitor.isDaemon());
    }

    @Test
    public void runAndTrackReferences() {
        referenceMonitor.start();

        boolean[] cleanUpDone = new boolean[100];
        for (int i = 0; i < cleanUpDone.length; i++) {
            cleanUpDone[i] = false;
            int finalI = i;
            referenceMonitor.track(new Object(), () -> cleanUpDone[finalI] = true);
        }

        System.gc();
        MemoryTimeWaster.waste(20000000);
        System.gc();

        for (int i = 0; i < cleanUpDone.length; i++)
        {
            assertTrue(cleanUpDone[i], "Clean up " + i);
        }

        referenceMonitor.interrupt();

        try {
            referenceMonitor.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }
}
