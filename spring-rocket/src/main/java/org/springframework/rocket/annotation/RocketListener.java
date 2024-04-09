package org.springframework.rocket.annotation;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.rocket.core.FilterExpressionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@MessageMapping
@Documented
@Repeatable(RocketListeners.class)
public @interface RocketListener {

    /**
     * The unique identifier of the container for this listener.
     * <p>If none is specified an auto-generated id is used.
     * <p>SpEL {@code #{...}} and property placeholders {@code ${...}} are supported.
     * @return the {@code id} for the container managing for this endpoint.
     * @see org.springframework.rocket.config.RocketListenerEndpointRegistry#getListenerContainer(String)
     */
    String id() default "";

    /**
     * Override the {@code groupId} property for the consumer factory with this value
     * for this listener only.
     * <p>SpEL {@code #{...}} and property placeholders {@code ${...}} are supported.
     * @return the group id.
     */
    String groupId() default "";

    String topic();

    FilterExpressionType filterExpressionType() default FilterExpressionType.TAG;

    String filterExpression() default "*";

    /**
     * The bean name of the {@link org.springframework.rocket.config.ListenerContainerFactory}
     * to use to create the message listener container responsible to serve this endpoint.
     * <p>
     * If not specified, the default container factory is used, if any.
     * @return the container factory bean name.
     */
    String containerFactory() default "";

    /**
     * Set to true or false, to override the default setting in the container factory. May
     * be a property placeholder or SpEL expression that evaluates to a {@link Boolean} or
     * a {@link String}, in which case the {@link Boolean#parseBoolean(String)} is used to
     * obtain the value.
     * <p>
     * SpEL {@code #{...}} and property placeholders {@code ${...}} are supported.
     * @return true to auto start, false to not auto start.
     */
    String autoStartup() default "";

    /**
     * Activate batch consumption.
     * @return whether this listener is in batch mode or not.
     */
    boolean batch() default false;

    /**
     * Set to true or false, to override the default setting in the container factory. May
     * be a property placeholder or SpEL expression that evaluates to a {@link Boolean} or
     * a {@link String}, in which case the {@link Boolean#parseBoolean(String)} is used to
     * obtain the value.
     * <p>
     * SpEL {@code #{...}} and property placeholders {@code ${...}} are supported.
     * @return true to consume concurrently, otherwise consume orderly
     */
    String concurrency() default "true";

    String[] properties() default {};

}
