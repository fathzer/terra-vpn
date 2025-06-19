package com.fathzer.terravpn.ssh;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRunnerTest {

    @Test
    void testProgressCallbackIsCalledForEachLine() throws IOException {
        Ssh ssh = mock(Ssh.class);
        when(ssh.exec(anyString(), any(OutputStream.class), any(OutputStream.class))).thenAnswer(invocation -> {
            // Simulate what the remote server would output
            OutputStream out = invocation.getArgument(1);
            out.write(("LINE_START:1\nOutput 1-A\nOutput 1-B\nLINE_START:2\nOutput 2-A\n").getBytes());
            return 0;
        });

        ScriptRunner runner = new ScriptRunner(ssh);
        List<String> script = Arrays.asList("cmd1", "cmd2");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicInteger lastLine = new AtomicInteger(0);

        int status = runner.exec(script, stdout, stderr, lastLine::set);

        assertEquals(0, status);
        assertEquals(3, stdout.toString().split("\n").length);
        assertEquals(2, lastLine.get(), "Callback should have been called with LINE_START:2");
    }
}