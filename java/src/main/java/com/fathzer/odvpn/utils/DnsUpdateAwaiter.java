package com.fathzer.odvpn.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;

public class DnsUpdateAwaiter {
	private final int maxAttempts;
	private final long sleepTimeMs;
	
    public DnsUpdateAwaiter(int maxAttempts, long sleepTimeMs) {
		this.maxAttempts = maxAttempts;
		this.sleepTimeMs = sleepTimeMs;
	}
	
    public void waitFor(String hostName, String ip) throws IOException {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                InetAddress address = InetAddress.getByName(hostName);
                String resolvedIp = address.getHostAddress();
                if (ip.equals(resolvedIp)) {
                    return;
                }
                logAttempt(attempt, resolvedIp);
            } catch (Exception e) {
                logException(attempt, e);
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
        }
        throw new IOException(String.format("Timeout waiting for DNS propagation of %s to %s", hostName, ip));
    }

    protected void logAttempt(int attempt, String resolvedIp) {
    }

    protected void logException(int attempt, Exception e) {
    }
}
