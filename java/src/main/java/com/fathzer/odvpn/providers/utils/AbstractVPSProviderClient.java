package com.fathzer.odvpn.providers.utils;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.http.Request;
import com.fathzer.http.RequestDecorator;
import com.fathzer.http.RestClient;

public abstract class AbstractVPSProviderClient implements AutoCloseable {
    private RestClient client;
    protected final ObjectMapper objectMapper;
    private final RequestDecorator authentication;
    
    protected AbstractVPSProviderClient(RequestDecorator authentication) {
        this.client = null;
        this.objectMapper = new ObjectMapper();
        this.authentication = authentication;
    }

    /**
     * Gets the REST client used by this client.
     * @return the REST client
     */
    public RestClient getRestClient() {
        if (this.client == null) {
            this.client = new RestClient() {
                @Override
                protected <T> T deserializeResponse(@Nonnull String response, @Nonnull Class<T> responseType) throws IOException {
                    return objectMapper.readValue(response, responseType);
                }
                @Override
                protected String serializeRequest(@Nonnull Object body) throws IOException {
                    return objectMapper.writeValueAsString(body);
                }
            };
            this.client.withDecorator(this.authentication);
            this.client.withDecorator(RequestDecorator.acceptJson());
            this.client.withDecorator(RequestDecorator.sendJson());
        }
        return this.client;
    }
    

    public String execute(Request request) throws IOException {
        return this.getRestClient().execute(request, String.class).body();
    }

    /**
     * Sets the REST client to be used by this client.
     * @param client the REST client
     */
    public void setRestClient(RestClient client) {
        if (this.client!=null) this.client.close();
        this.client = client;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

}
