package com.fathzer.terravpn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

public final class ProviderRegistry {
    private static final Map<String, Provider> providers = new HashMap<>();
    private static boolean initialized = false;
    
    private ProviderRegistry() {}
    
    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        Reflections reflections = new Reflections("com.fathzer.terravpn");
        Set<Class<? extends Provider>> impls = reflections.getSubTypesOf(Provider.class);
        for (Class<? extends Provider> impl : impls) {
            if (impl.isInterface()) {
                continue;
            }
            try {
                Provider provider = impl.getDeclaredConstructor().newInstance();
                provider = providers.put(provider.id(), provider);
                if (provider != null) {
                    throw new InitializationException("Duplicate provider ID " + provider.id());
                }
            } catch (Exception e) {
                throw new InitializationException("Failed to initialize provider " + impl, e);
            }
        }
        initialized = true;
    }
    
    public static Provider getProvider(String id) {
        ensureInitialized();
        final Provider provider = providers.get(id);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider ID " + id);
        }
        return provider;
    }
}
