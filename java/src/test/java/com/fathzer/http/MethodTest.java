package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MethodTest {

    @ParameterizedTest
    @EnumSource(Method.class)
    void testHasBody(Method method) {
        // Test that hasBody() returns the expected value for each HTTP method
        switch (method) {
            case GET, HEAD, DELETE, OPTIONS:
                assertFalse(method.hasBody(), method + " should not have a body");
                break;
            case POST, PUT, PATCH:
                assertTrue(method.hasBody(), method + " should have a body");
                break;
            default:
                fail("Unhandled method: " + method);
        }
    }

    @Test
    void testValues() {
        // Test that all expected HTTP methods are present
        Method[] methods = Method.values();
        assertEquals(7, methods.length, "Unexpected number of HTTP methods");
        assertEquals(Set.of(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.PATCH, Method.HEAD, Method.OPTIONS),
            Set.of(methods)
        );
    }

    @Test
    void testBodyMethods() {
        // Test that body-related methods behave as expected
        assertTrue(Method.POST.hasBody());
        assertFalse(Method.GET.hasBody());
        assertFalse(Method.HEAD.hasBody());
        assertTrue(Method.PUT.hasBody());
        assertFalse(Method.DELETE.hasBody());
        assertTrue(Method.PATCH.hasBody());
        assertFalse(Method.OPTIONS.hasBody());
    }
}
