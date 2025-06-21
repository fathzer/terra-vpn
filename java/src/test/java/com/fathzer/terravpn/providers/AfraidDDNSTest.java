package com.fathzer.terravpn.providers;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AfraidDDNSTest {

    @Test
    void testUpdateDns() throws Exception {
        @SuppressWarnings("unchecked")
        final HttpResponse<String> response = mock(HttpResponse.class);
        final AtomicReference<HttpRequest> lastRequest = new AtomicReference<>();
        final AfraidDDNS afraidDDNS = new AfraidDDNS() {
            @Override
            protected HttpResponse<String> doRequest(final HttpRequest request) throws IOException, InterruptedException {
                lastRequest.set(request);
                return response;
            }
        };
        Map<String, String> config = Map.of(AfraidDDNS.VAR_TOKEN, "token");

        when(response.statusCode()).thenReturn(200);

        // Check for "address not changed" error message
        when(response.body()).thenReturn("ERROR: Address 127.0.0.1 has not changed.");
        assertDoesNotThrow(() -> afraidDDNS.updateDns(config, "useless", "127.0.0.1"));
        assertEquals("https://freedns.afraid.org/dynamic/update.php?token&address=127.0.0.1", lastRequest.get().uri().toString());

        // Check address was successfully changed
        when(response.body()).thenReturn("Updated 1 host(s) terravpn.soon.it to 127.0.0.1 in 0.006 seconds");
        assertDoesNotThrow(() -> afraidDDNS.updateDns(config, "useless", "127.0.0.1"));

        // Check for other error messages
        when(response.body()).thenReturn("bad 127.0.0.1");
        assertThrows(IOException.class, () -> afraidDDNS.updateDns(config, "useless", "127.0.0.1"));

        // Check for error codes != 200
        when(response.statusCode()).thenReturn(401);
        assertThrows(IOException.class, () -> afraidDDNS.updateDns(config, "useless", "127.0.0.1"));
    }
}
