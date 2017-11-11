package com.minemaarten.signals.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;

/**
 * Annotation that should be used on {@link IRail}, {@link IRailMapper} and {@link IDestinationProvider} implementations.
 * @author Maarten
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Signals{

}
