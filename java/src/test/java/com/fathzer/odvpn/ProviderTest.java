package com.fathzer.odvpn;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProviderTest {
    private static class TestProvider extends Provider<String> {
        private Map<String, String> envVars;

        public TestProvider(Map<String, String> envVars) {
            this.envVars = envVars;
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public Class<String> getConfigClass() {
            return String.class;
        }

        @Override
        public String getValue(String variable) {
            return envVars.get(variable);
        }
    }

    @Test
    void testSettings() {
        final TestProvider provider = new TestProvider(Map.of());
        provider.setSettings("value");
        
        // When/Then
        assertEquals("value", provider.getSettings());
    }

    @Test
    void testResolve() {
        TestProvider provider = new TestProvider(
            Map.of(
                "VAR1", "value1",
                "VAR2", "value2"
            )
        );
        assertEquals("value1", provider.resolve("${VAR1}"));
        assertEquals("value2", provider.resolve("${VAR2}"));
        assertEquals("static_value", provider.resolve("static_value"));
        assertEquals("${UNKNOWN_VAR}", provider.resolve("${UNKNOWN_VAR}"));
        assertNull(provider.resolve(null));
    }
}
