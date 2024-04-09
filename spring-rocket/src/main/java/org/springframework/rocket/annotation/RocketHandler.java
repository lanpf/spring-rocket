package org.springframework.rocket.annotation;

import org.springframework.messaging.handler.annotation.MessageMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MessageMapping
@Documented
public @interface RocketHandler {

    /**
     * When true, designate that this is the default fallback method if the payload type
     * matches no other {@link RocketHandler} method. Only one method can be so designated.
     * @return true if this is the default method.
     */
    boolean isDefault() default false;
}
