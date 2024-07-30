package com.scalefocus.mile.jms.auth.poc.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to indicate that a class is thread-safe.
 *
 * <p>This annotation serves as a documentation tool to inform developers
 * that the annotated class can be safely used by multiple threads concurrently
 * without introducing concurrency issues. It does not enforce any thread-safety
 * guarantees at the runtime level.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ThreadSafe {
}
