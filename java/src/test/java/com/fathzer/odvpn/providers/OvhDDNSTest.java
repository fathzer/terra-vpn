package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OvhDDNSTest {

    @Test
    void testUpdateDns() {
        @SuppressWarnings("unchecked")
        final HttpResponse<String> response = mock(HttpResponse.class);
        final AtomicReference<HttpRequest> lastRequest = new AtomicReference<>();
        final OvhDDNS ovhDDNS = new OvhDDNS() {
            @Override
            protected HttpResponse<String> doRequest(final HttpRequest request) throws IOException {
                lastRequest.set(request);
                return response;
            }
        };

        ovhDDNS.setSettings(new OvhDDNS.Settings("user", "password"));

        when(response.statusCode()).thenReturn(200);

        // Check for "address not changed" error message
        when(response.body()).thenReturn("nochg 127.0.0.1");
        assertDoesNotThrow(() -> ovhDDNS.updateDns("hostname", "127.0.0.1"));
        assertEquals("https://www.ovh.com/nic/update?system=dyndns&hostname=hostname&myip=127.0.0.1", lastRequest.get().uri().toString());
        assertEquals("Basic dXNlcjpwYXNzd29yZA==", lastRequest.get().headers().firstValue("Authorization").get());

        // Check address was successfully changed
        when(response.body()).thenReturn("good 127.0.0.1");
        assertDoesNotThrow(() -> ovhDDNS.updateDns("hostname", "127.0.0.1"));

        // Check for other error messages
        when(response.body()).thenReturn("ERROR: Some other error");
        assertThrows(IOException.class, () -> ovhDDNS.updateDns("hostname", "127.0.0.1"));

        // Check for error codes != 200
        when(response.statusCode()).thenReturn(401);
        assertThrows(IOException.class, () -> ovhDDNS.updateDns("hostname", "127.0.0.1"));
    }
}
