package com.fathzer.odvpn.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ObjectConfigTest {
    private static class TestObjectConfig extends ObjectConfig<String> {
        private final Map<String, String> testEnvVars;

        public TestObjectConfig(String provider, Map<String, String> rawConfig, Map<String, String> testEnvVars) {
            super(provider, rawConfig);
            this.testEnvVars = testEnvVars;
        }

        @Override
        protected String get(String variable) {
            return testEnvVars.get(variable);
        }
    }

    @Test
    void testResolve_NoVariable() {
        // Given
        TestObjectConfig config = new TestObjectConfig("test", Map.of("key", "value"), Map.of());
        
        // When/Then
        assertEquals("value", config.rawConfig().get("key"));
        assertEquals("value", config.config().get("key"));
    }

    @Test
    void testResolve_MultipleVariables() {
        TestObjectConfig config = new TestObjectConfig("test", 
            Map.of(
                "key1", "${VAR1}",
                "key2", "${VAR2}",
                "key3", "static_value",
                "key4", "${UNKNOWN_VAR}"    
            ), 
            Map.of(
                "VAR1", "value1",
                "VAR2", "value2"
            )
        );
        
        Map<String, String> resolved = config.config();
        
        assertEquals("value1", resolved.get("key1"));
        assertEquals("value2", resolved.get("key2"));
        assertEquals("static_value", resolved.get("key3"));
        assertEquals("${UNKNOWN_VAR}", resolved.get("key4"));
    }

    @Test
    void testConfig_IsCached() {
        TestObjectConfig config = new TestObjectConfig("test", 
            Map.of("key", "${VAR}"), 
            Map.of("VAR", "value")
        );
        
        Map<String, String> firstCall = config.config();
        Map<String, String> secondCall = config.config();
        
        assertSame(firstCall, secondCall, "config() should return the same map instance on subsequent calls");
    }

    @Test
    void testProviderAndRawConfig() {
        Map<String, String> rawConfig = Map.of("key", "value");
        TestObjectConfig config = new TestObjectConfig("test_provider", rawConfig, Map.of());
        
        assertEquals("test_provider", config.provider());
        assertEquals(rawConfig, config.rawConfig(), "The content of the maps should be equal");
        assertNotSame(rawConfig, config.rawConfig(), "The maps should be different instances as ObjectConfig creates an unmodifiable copy");
    }
    
    @Test
    void testReturnedMapsAreImmutable() {
        // Given
        Map<String, String> rawConfig = new java.util.HashMap<>();
        rawConfig.put("key", "value");
        TestObjectConfig config = new TestObjectConfig("test", rawConfig, Map.of());
        
        checkImmutable(config.rawConfig(), "key");
        checkImmutable(config.config(), "key");
    }

    private void checkImmutable(Map<String, String> map, String key) {
        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", "newValue"));
        assertThrows(UnsupportedOperationException.class, () -> map.remove(key));
        assertThrows(UnsupportedOperationException.class, map::clear);
    }
}
