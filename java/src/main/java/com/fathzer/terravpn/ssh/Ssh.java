package com.fathzer.terravpn.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class Ssh implements AutoCloseable {
    private Session session;

    public static class Builder {
        private final String host;
        private int port = 22;
        private final String keyPath;
        private String user;
        private String keyPassword;
        private int maxTryCount = 1;
        private int pauseBetweenRetries = 5000;
        private IntConsumer onRetry = attempt -> {};

        public Builder(String host, String keyPath) {
            this.host = host;
            this.user = "root";
            this.keyPath = keyPath;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder keyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public Builder maxTryCount(int maxTryCount) {
            this.maxTryCount = maxTryCount;
            return this;
        }

        public Builder pauseBetweenRetries(int pauseBetweenRetries) {
            this.pauseBetweenRetries = pauseBetweenRetries;
            return this;
        }

        public Ssh build() throws IOException {
            return new Ssh(this.getSession());
        }

        private Session getSession() throws IOException {
            final Properties config = new java.util.Properties(); 
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            try {
                jsch.addIdentity(this.keyPath, this.keyPassword);
            } catch (JSchException e) {
                throw new IOException(e);
            }
            Session session = null;

            for (int attempt = 1; attempt <= this.maxTryCount; attempt++) {
                try {
                    session = jsch.getSession(user, host, this.port);
                    session.setConfig(config);
                    session.connect(this.pauseBetweenRetries);
                    return session;
                } catch (JSchException e) {
                    if (attempt == this.maxTryCount) {
                        throw new IOException(e);
                    }
                    this.onRetry.accept(attempt);
                    try {
                        Thread.sleep(this.pauseBetweenRetries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
            }
            // Unreachable but required by the compiler
            throw new IOException();
        }
    }

    private Ssh(Session session) {
        this.session = session;
    }

    public int exec(List<String> commands, OutputStream out, OutputStream err) throws IOException {
        return exec(commands.stream().collect(Collectors.joining("\n")), out, err);
    }

    public int exec(String command, OutputStream out, OutputStream err) throws IOException {
        try {
	    	final Channel channel=session.openChannel("exec");
            try {
                ((ChannelExec)channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec)channel).setErrStream(err, true);
                
                InputStream in=channel.getInputStream();
                channel.connect();
                byte[] tmp=new byte[1024];
                while(true){
                    while(in.available()>0){
                        int i=in.read(tmp, 0, 1024);
                        if (i<0) break;
                        out.write(tmp, 0, i);
                    }
                    if(channel.isClosed()){
                        return channel.getExitStatus();
                    }
                    pause();
                }
            } finally {
                channel.disconnect();
	        }
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    private void pause() throws InterruptedIOException {
        try {
            Thread.sleep(50); // 50ms pause to reduce CPU usage
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Thread interrupted");
        }
    }

    public void download(String remoteFile, String localFile) throws IOException {
        try {
            final ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
            try {
                channel.get(remoteFile, localFile);
            } finally {
                channel.exit();
            }
        } catch (JSchException | SftpException e) {
            throw new IOException(e);
        }
    }

    public void upload(String localFile, String remoteFile) throws IOException {
        try {
            final ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
            try {
                channel.put(localFile, remoteFile);
            } finally {
                channel.exit();
            }
        } catch (JSchException | SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        session.disconnect();
    }
}
