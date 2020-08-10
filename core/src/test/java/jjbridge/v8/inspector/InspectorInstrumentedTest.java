package jjbridge.v8.inspector;

import jjbridge.common.inspector.JSInspector;
import jjbridge.common.runtime.JSReference;
import jjbridge.common.runtime.JSRuntime;
import jjbridge.common.value.JSObject;
import jjbridge.v8.V8Engine;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.*;

public class InspectorInstrumentedTest {
    private JSRuntime runtime;
    private JSInspector inspector;
    private InspectorClient inspectorClient;

    private class InspectorClient extends WebSocketClient {
        private LinkedBlockingQueue<OnMessageExpectation> expectations;

        InspectorClient(URI serverUri) {
            super(serverUri);
            expectations = new LinkedBlockingQueue<>();
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
        }

        @Override
        public void onMessage(String message) {
            Exception toRethrow = null;
            try {
                OnMessageExpectation messageExpectation = expectations.take();
                try {
                    if (messageExpectation != null)
                        messageExpectation.check(message);
                } catch (Exception e) {
                    toRethrow = e;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (toRethrow != null) throw new RuntimeException(toRethrow);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
        }

        @Override
        public void onError(Exception ex) {
            throw new RuntimeException(ex);
        }

        public void sendAndExpect(String message, OnMessageExpectation... expectation) {
            send(message);
            for (OnMessageExpectation onMessageExpectation : expectation) {
                expectations.offer(onMessageExpectation);
            }
        }
    }

    private interface OnMessageExpectation {
        void check(String response);
    }

    @Before
    public final void before() throws URISyntaxException, InterruptedException {
        int port = 9088;

        V8Engine engine = new V8Engine();
        runtime = engine.newRuntime();
        try {
            inspector = engine.newInspector(port);
            inspectorClient = new InspectorClient(new URI("ws://127.0.0.1:" + port));
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("Connection failed");
        }

        int waitingTime = 5000;
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(waitingTime / 5);
                inspectorClient.connectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t.start();
        inspector.attach(runtime).waitForConnection(waitingTime);
        t.join();
    }

    @After
    public final void after() throws Exception {
        // wait for all messages to be processed
        CountDownLatch latch = new CountDownLatch(1);
        inspectorClient.sendAndExpect("{\"dummy\":\"message\"}", response -> latch.countDown());
        latch.await();

        inspectorClient.closeBlocking();
        inspector.detach();
        runtime.close();
    }

    @Test
    @Ignore("to be fixed")
    public final void interactionWithInspector() throws InterruptedException {
        inspectorClient.sendAndExpect(
                "{\"id\":1,\"method\":\"Profiler.enable\"}",
                response -> assertEquals("{\"id\":1,\"result\":{}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":2,\"method\":\"Runtime.enable\"}",
                response -> assertEquals("{\"method\":\"Runtime.executionContextCreated\",\"params\":{\"context\":{\"id\":1,\"origin\":\"\",\"name\":\"JJBridge-V8 Main Context\"}}}", response),
                response -> assertEquals("{\"id\":2,\"result\":{}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":3,\"method\":\"Debugger.enable\"}",
                response -> assertTrue(response.contains("{\"id\":3,\"result\":{\"debuggerId\":\"(")));

        inspectorClient.sendAndExpect(
                "{\"id\":4,\"method\":\"Debugger.setPauseOnExceptions\",\"params\":{\"state\":\"none\"}}",
                response -> assertEquals("{\"id\":4,\"result\":{}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":5,\"method\":\"Debugger.setAsyncCallStackDepth\",\"params\":{\"maxDepth\":32}}",
                response -> assertEquals("{\"id\":5,\"result\":{}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":6,\"method\":\"Debugger.setBlackboxPatterns\",\"params\":{\"patterns\":[]}}",
                response -> assertEquals("{\"id\":6,\"result\":{}}", response));

        CountDownLatch waitBeforeScript = new CountDownLatch(1);
        inspectorClient.sendAndExpect(
                "{\"id\":7,\"method\":\"Runtime.runIfWaitingForDebugger\"}",
                response -> {
                    assertEquals("{\"id\":7,\"result\":{}}", response);
                    waitBeforeScript.countDown();
                },
                response -> assertEquals("{\"method\":\"Debugger.scriptParsed\",\"params\":{\"scriptId\":\"10\",\"url\":\"/script_0\",\"startLine\":0,\"startColumn\":0,\"endLine\":0,\"endColumn\":66,\"executionContextId\":1,\"hash\":\"2f160716878340dd7c1e0066bcd32d7a37d6431e\",\"isLiveEdit\":false,\"sourceMapURL\":\"file:///script_0\",\"hasSourceURL\":false,\"isModule\":false,\"length\":66}}", response));

        waitBeforeScript.await();
        String scriptSource = "class AAA { str() { console.log('AAA'); return 1000 } }; new AAA()";
        JSReference script1 = runtime.executeScript(scriptSource);
        JSObject aaa = runtime.resolveReference(script1);

        inspectorClient.sendAndExpect(
                "{\"id\":8,\"method\":\"Debugger.getScriptSource\",\"params\":{\"scriptId\":\"10\"}}",
                response -> assertEquals("{\"id\":8,\"result\":{\"scriptSource\":\"class AAA { str() { console.log('AAA'); return 1000 } }; new AAA()\"}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":9,\"method\":\"Debugger.getPossibleBreakpoints\",\"params\":{\"start\":{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":20},\"end\":{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":39},\"restrictToFunction\":false}}",
                response -> assertEquals("{\"id\":9,\"result\":{\"locations\":[{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":20},{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":28,\"type\":\"call\"}]}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":10,\"method\":\"Debugger.setBreakpointsActive\",\"params\":{\"active\":true}}",
                response -> assertEquals("{\"id\":10,\"result\":{}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":11,\"method\":\"Debugger.getPossibleBreakpoints\",\"params\":{\"start\":{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":20},\"end\":{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":40},\"restrictToFunction\":false}}",
                response -> assertEquals("{\"id\":11,\"result\":{\"locations\":[{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":20},{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":28,\"type\":\"call\"}]}}", response));

        inspectorClient.sendAndExpect(
                "{\"id\":12,\"method\":\"Debugger.setBreakpointByUrl\",\"params\":{\"lineNumber\":0,\"urlRegex\":\"/script_0|file:///script_0\",\"columnNumber\":20,\"condition\":\"\"}}",
                response -> assertEquals("{\"id\":12,\"result\":{\"breakpointId\":\"2:0:20:/script_0|file:///script_0\",\"locations\":[{\"scriptId\":\"10\",\"lineNumber\":0,\"columnNumber\":20}]}}", response));
    }
}
