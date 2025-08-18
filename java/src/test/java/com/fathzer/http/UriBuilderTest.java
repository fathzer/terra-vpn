package com.fathzer.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

@DisplayName("UriBuilder Tests")
class UriBuilderTest {
    private static final String BASE_URI = "https://example.com";

    @Nested
    @DisplayName("From URI Construction")
    class FromUriConstruction {

        @Test
        @DisplayName("Should parse existing URI correctly")
        void shouldParseExistingUri() {
            String baseUri = BASE_URI + ":8080/v1/users?format=json&active=true";
            URI uri = new UriBuilder(baseUri).pathSegment("123").queryParam("details", "full").build();

            assertEquals("https://example.com:8080/v1/users/123?format=json&active=true&details=full", 
                        uri.toString());
        }

        @Test
        @DisplayName("Should handle URI without query parameters")
        void shouldHandleUriWithoutQuery() {
            String baseUri = BASE_URI + "/api/v1";
            URI uri = new UriBuilder(baseUri).pathSegment("users").queryParam("limit", "10").build();
            assertEquals("https://example.com/api/v1/users?limit=10", uri.toString());
        }

        @Test
        @DisplayName("Should handle URI without path")
        void shouldHandleUriWithoutPath() {
            String baseUri = BASE_URI;
            URI uri = new UriBuilder(baseUri).pathSegment("api").queryParam("version", "v1").build();
            assertEquals("https://example.com/api?version=v1", uri.toString());
        }

        @Test
        @DisplayName("Should throw exception for invalid URI")
        void shouldThrowExceptionForInvalidUri() {
            assertThrows(IllegalArgumentException.class, () -> new UriBuilder("not a valid uri"));
        }

        @Test
        @DisplayName("Should preserve empty path segments from existing URI")
        void shouldPreserveEmptyPathSegmentsFromUri() {
            URI uri = new UriBuilder(BASE_URI + "/api//users").pathSegment("123").build();
            assertEquals("https://example.com/api//users/123", uri.toString());
        }

        @Test
        @DisplayName("Should handle multiple values for same query param from existing URI")
        void shouldHandleMultipleQueryValuesFromUri() {
            URI uri = new UriBuilder(BASE_URI + "/search?tags=java&tags=spring&format=json").queryParam("tags", "web").build();
            String result = uri.toString();
            assertTrue(result.contains("tags=java"));
            assertTrue(result.contains("tags=spring"));
            assertTrue(result.contains("tags=web"));
            assertTrue(result.contains("format=json"));
        }
    }

    @Nested
    @DisplayName("Null Base URI")
    @SuppressWarnings("null")
    class NullHandling {
        UriBuilder builder = new UriBuilder(BASE_URI);

        @Test
        @DisplayName("Should throw exception for null base URI")
        void shouldThrowExceptionForNullBaseUri() {
            assertThrows(NullPointerException.class, () -> new UriBuilder(null));
        }
        @Test
        @DisplayName("Should throw exception for null segments")
        void shouldThrowExceptionForNullSegments() {
            assertThrows(NullPointerException.class, () -> builder.pathSegment(null));
        }
        @Test
        @DisplayName("Should throw exception for null query params values")
        void shouldThrowExceptionForNullQueryParamsValues() {
            assertThrows(NullPointerException.class, () -> builder.queryParam("test", null));
        }
        @Test
        @DisplayName("Should throw exception for null query param name")
        void shouldThrowExceptionForNullQueryParamName() {
            assertThrows(NullPointerException.class, () -> builder.queryParam(null, "value"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        @DisplayName("Should handle special characters in all components")
        void shouldHandleSpecialCharacters() {
            URI uri = new UriBuilder(BASE_URI).pathSegment("search").pathSegment("user@domain.com")
            .pathSegment("1/3").queryParam("filter", "name=John&age>25").build();
            assertEquals("https://example.com/search/user%40domain.com/1%2F3?filter=name%3DJohn%26age%3E25", uri.toString());

            URI uri2 = new UriBuilder(BASE_URI).pathSegment("/search/").pathSegment("x").build();
            assertEquals("https://example.com/%2Fsearch%2F/x", uri2.toString());
        }

        @Test
        @DisplayName("Should create valid URI string representation")
        void shouldCreateValidStringRepresentation() {
            UriBuilder builder = new UriBuilder(BASE_URI).pathSegment("api").queryParam("test", "value");
            assertEquals("https://example.com/api?test=value", builder.toString());
            
            // Should be parseable as a valid URI
            assertDoesNotThrow(() -> URI.create(builder.toString()));
        }

        @Test
        @DisplayName("Should accept trailing / in base URI")
        void shouldAcceptTrailingSlashInBaseUri() {
            URI uri = new UriBuilder(BASE_URI + "/").pathSegment("api").queryParam("test", "value").build();
            assertEquals("https://example.com//api?test=value", uri.toString());
        }
    }
}
