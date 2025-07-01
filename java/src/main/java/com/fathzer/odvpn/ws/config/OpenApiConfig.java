package com.fathzer.odvpn.ws.config;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    /**
     * Fixes the multipart/form-data encoding for the /api/vpns/{id} endpoint
     */
    public OpenApiCustomizer fixMultipartJsonEncoding() {
        return openApi -> {
            PathItem pathItem = openApi.getPaths().get("/api/vpns/{id}");
            if (pathItem != null && pathItem.getPost() != null) {
                RequestBody requestBody = pathItem.getPost().getRequestBody();
                if (requestBody != null && requestBody.getContent() != null) {
                    MediaType mediaType = requestBody.getContent()
                        .get("multipart/form-data");
                    if (mediaType != null) {
                        if (mediaType.getEncoding() == null) {
                            mediaType.setEncoding(new java.util.LinkedHashMap<>());
                        }
                        mediaType.getEncoding().put("config",
                                new Encoding().contentType("application/json"));
                    }
                }
            }
        };
    }
}

