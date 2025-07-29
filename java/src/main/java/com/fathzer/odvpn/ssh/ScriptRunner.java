package com.fathzer.odvpn.ssh;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.utils.AbstractLineBasedOutputStream;

/**
 * Runs a script on a remote server and provides progress updates.
 */
public class ScriptRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptRunner.class);

    private final Ssh baseExecutor;
    private final String startMarker;

    class ProgressAwareOutputStream extends AbstractLineBasedOutputStream {
        private final OutputStream delegate;
        private final Consumer<Integer> callback;

        public ProgressAwareOutputStream(OutputStream delegate, Consumer<Integer> callback) {
            this.delegate = delegate;
            this.callback = callback;
        }

        @Override
        protected void doFlush() {
            flushBuffer();
            try {
                delegate.flush();
            } catch (IOException e) {
                LOGGER.warn("Failed to flush delegate", e);
            }
        }

        private void flushBuffer() {
            final String line = buffer.toString();
            if (line.startsWith(startMarker)) {
                try {
                    int step = Integer.parseInt(line.substring(startMarker.length()).trim());
                    callback.accept(step);
                } catch (NumberFormatException ignored) {
                    LOGGER.warn("Failed to parse progress marker: {}", line);
                }
            } else {
                try {
                    delegate.write(line.getBytes());
                    delegate.write('\n');
                    delegate.flush();
                } catch (IOException e) {
                    LOGGER.warn("Failed to write delegate", e);
                }
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                delegate.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close delegate", e);
            }
        }
    }

    /**
     * Creates a new ScriptRunner with the default start marker.
     * @param executor the SSH executor
     * @see #ScriptRunner(Ssh, String)
     */
    public ScriptRunner(Ssh executor) {
        this(executor, "LINE_START:");
    }

    /**
     * Creates a new ScriptRunner with a custom start marker.
     * @param executor the SSH executor
     * @param startMarker the start marker. An echo instruction of this marker is inserted before each line of the script,
     * followed by the line number. The progress callback is called when this marker is found in stdout.<br>
     * The default marker is "LINE_START:"
     */
    public ScriptRunner(Ssh executor, String startMarker) {
        this.baseExecutor = executor;
        this.startMarker = startMarker;
    }

    /**
     * Executes the script on the remote server.
     * @param scriptLines the script lines
     * @param out the output stream
     * @param err the error stream
     * @param progressCallback the progress callback that is called before each line of the script is executed
     * @return the exit code of the script
     * @throws IOException if an I/O error occurs
     */
    public int exec(List<String> scriptLines, OutputStream out, OutputStream err, Consumer<Integer> progressCallback) throws IOException {
        String instrumentedScript = instrumentScript(scriptLines);
        OutputStream decoratedOut = new ProgressAwareOutputStream(out, progressCallback);
        return baseExecutor.exec(instrumentedScript, decoratedOut, err);
    }

    private String instrumentScript(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append("echo LINE_START:").append(i + 1).append(" && ").append(lines.get(i)).append("\n");
        }
        return sb.toString();
    }
}