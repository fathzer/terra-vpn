package com.fathzer.odvpn;

import com.fathzer.odvpn.utils.ClassRegistry;

public final class ProviderRegistry {
    private static final ClassRegistry registry = new ClassRegistry("com.fathzer.odvpn.providers");
    
    private ProviderRegistry() {}
    
    public static <T> T getProvider(String id, Class<T> type) {
        return registry.getProvider(id, type);
    }
}
