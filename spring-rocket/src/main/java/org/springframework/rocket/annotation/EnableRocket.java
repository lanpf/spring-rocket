package org.springframework.rocket.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables detection of {@link RocketListener} annotations on any Spring-managed bean in
 * the container.
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RocketAnnotationConfigurationSelector.class)
public @interface EnableRocket {
}
