package jjbridge.v8;

import jjbridge.v8.inspector.Inspector;
import jjbridge.v8.runtime.Runtime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class V8EngineTest {
    private V8Engine engine;

    @Before
    public final void before() {
        engine = new V8Engine();
    }

    @Test
    public void newRuntime() {
        assertTrue(engine.newRuntime() instanceof Runtime);
    }

    @Test
    public void newInspector() {
        assertTrue(engine.newInspector(1000) instanceof Inspector);
    }
}
