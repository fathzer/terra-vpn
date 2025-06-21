package com.fathzer.terravpn;

import com.fathzer.terravpn.utils.ClassRegistry;

public final class ProviderRegistry {
    private static final ClassRegistry registry = new ClassRegistry("com.fathzer.terravpn.providers");
    
    private ProviderRegistry() {}
    
    public static <T> T getProvider(String id, Class<T> type) {
        return registry.getProvider(id, type);
    }
}
