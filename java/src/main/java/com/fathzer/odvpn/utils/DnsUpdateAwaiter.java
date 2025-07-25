package com.fathzer.odvpn.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;

/**
 * A utility class to wait for DNS propagation of a host name to an IP address.
 */
public class DnsUpdateAwaiter {
	private final int maxAttempts;
	private final long sleepTimeMs;
	
    /**
     * Creates a new DnsUpdateAwaiter.
     * @param maxAttempts the maximum number of attempts
     * @param sleepTimeMs the sleep time between attempts in milliseconds
     */
    public DnsUpdateAwaiter(int maxAttempts, long sleepTimeMs) {
		this.maxAttempts = maxAttempts;
		this.sleepTimeMs = sleepTimeMs;
	}
	
    /**
     * Waits for DNS propagation of a host name to an IP address.
     * @param hostName the host name
     * @param ip the IP address
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Logs an attempt to resolve the host name.
     * The default implementation does nothing.
     * @param attempt the attempt number
     * @param resolvedIp the resolved IP address
     */
    protected void logAttempt(int attempt, String resolvedIp) {
        // Does nothing by default
    }

    /**
     * Logs an exception that occurred during the DNS update process.
     * The default implementation does nothing.
     * @param attempt the attempt number
     * @param e the exception
     */
    protected void logException(int attempt, Exception e) {
        // Does nothing by default
    }
}
