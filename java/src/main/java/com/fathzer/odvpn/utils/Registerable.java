package com.fathzer.odvpn.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as registerable in the ProviderRegistry.
 * Only classes directly annotated with @Registerable will be registered.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registerable {
    /**
     * The id of the registered component.
     * @return the identifier.
     */
    String value();
    
    /**
     * Additional classes to register along with the annotated class.
     * This is useful when a provider needs to register additional related classes.
     * @return array of classes to register.
     */
    Class<?>[] classes();
}
