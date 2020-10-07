package jjbridge.engine.v8;

import jjbridge.api.inspector.JSInspector;
import jjbridge.engine.v8.inspector.Inspector;
import jjbridge.engine.v8.runtime.Runtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class V8EngineTest {
    private V8Engine engine;

    @BeforeEach
    public final void before() {
        engine = new V8Engine();
    }

    @Test
    public void newRuntime() {
        assertTrue(engine.newRuntime() instanceof Runtime);
    }

    @Test
    public void newInspector() {
        JSInspector jsInspector = engine.newInspector(1000);
        assertTrue(jsInspector instanceof Inspector);
        assertEquals(1000, ((Inspector) jsInspector).getPort());
    }
}
