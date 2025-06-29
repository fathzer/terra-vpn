package com.fathzer.odvpn.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

public final class ClassRegistry {
    private final Reflections reflections;
    private final Map<Class<?>, Map<String, Object>> providers = new HashMap<>();
    private boolean initialized = false;
    
    public ClassRegistry(String packageName) {
        this.reflections = new Reflections(packageName);
    }
    
    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        // Find all classes directly annotated with @Registerable
        Set<Class<?>> registerableClasses = reflections.getTypesAnnotatedWith(Registerable.class, true);
        
        for (Class<?> clazz : registerableClasses) {
            try {
                // Create instance and get its type
                Object provider = clazz.getDeclaredConstructor().newInstance();
                
                // Get the Registerable annotation
                Registerable registerable = clazz.getAnnotation(Registerable.class);
                
                // Add to class registries specified in the annotation
                for (Class<?> additionalClass : registerable.classes()) {
                    if (additionalClass.isAssignableFrom(provider.getClass())) {
                        Map<String, Object> additionalMap = providers.computeIfAbsent(additionalClass, k -> new HashMap<>());
                        additionalMap.put(registerable.value(), provider);
                    } else {
                        throw new InitializationException("Registerable class " + provider.getClass().getName() + " is not a subclass of " + additionalClass.getName());
                    }
                }
                
            } catch (Exception e) {
                throw new InitializationException("Failed to initialize provider " + clazz.getName(), e);
            }
        }
        
        initialized = true;
    }
    
    public <T> T getProvider(String id, Class<T> type) {
        ensureInitialized();
        Map<String, Object> map = providers.get(type);
        if (map == null) {
            throw new IllegalArgumentException("Unknown provider type " + type);
        }
        final Object provider = map.get(id);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider ID " + id);
        }
        return type.cast(provider);
    }
}
