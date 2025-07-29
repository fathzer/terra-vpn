package com.fathzer.odvpn.ssh;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.utils.ListOutputStream;

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
        final Ssh ssh = mock(Ssh.class);
        when(ssh.exec(anyString(), any(OutputStream.class), any(OutputStream.class))).thenAnswer(invocation -> {
            // Simulate what the remote server would output
            final OutputStream out = invocation.getArgument(1);
            out.write(("LINE_START:0\nOutput 1-A\nOutput 1-B\nLINE_START:1\nOutput 2-A\n").getBytes());
            return 0;
        });

        ScriptRunner runner = new ScriptRunner(ssh);
        List<String> script = Arrays.asList("cmd1", "cmd2");

        final ListOutputStream stdout = new ListOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final AtomicInteger lastLine = new AtomicInteger(0);

        int status = runner.exec(script, stdout, stderr, i -> {
            lastLine.set(i);
            if (i == 0) {
                assertTrue(stdout.getLines().isEmpty());
            } else if (i == 1) {
                assertEquals(List.of("Output 1-A","Output 1-B"), stdout.getLines());
            } else {
                fail("Unexpected progress callback");
            }
        });

        assertEquals(0, status);
        assertEquals(List.of("Output 1-A","Output 1-B","Output 2-A"), stdout.getLines());
        assertEquals("", stderr.toString());
        assertEquals(1, lastLine.get(), "Last callback should have been called with LINE_START:1");
    }
}