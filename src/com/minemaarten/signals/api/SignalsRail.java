package com.minemaarten.signals.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that should be used on {@link IRail} and {@link IRailMapper} implementations.
 * @author Maarten
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SignalsRail {
	
}
