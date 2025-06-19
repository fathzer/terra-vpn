package com.fathzer.terravpn.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
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

    private static Session getSession(String host, String user, String keyPath, String keyPassword) throws IOException {
        try {
            final Properties config = new java.util.Properties(); 
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            jsch.addIdentity(keyPath, (String)null);


            Session session=jsch.getSession(user, host, 22);
            session.setConfig(config);
            session.connect();
            return session;
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    public Ssh(String host, String user, String keyPath, String keyPassword) throws IOException {
        this.session = getSession(host, user, keyPath, keyPassword);
    }

    public Ssh(String host, String keyPath) throws IOException {
        this(host, "root", keyPath, null);
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
                ((ChannelExec)channel).setErrStream(err);
                
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
                }
            } finally {
                channel.disconnect();
	        }
        } catch (JSchException e) {
            throw new IOException(e);
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
