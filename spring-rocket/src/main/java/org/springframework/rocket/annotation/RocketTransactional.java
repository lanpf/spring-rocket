package org.springframework.rocket.annotation;

import org.springframework.rocket.config.RocketSupportBeanNames;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RocketTransactional {

    String topic();

    String[] properties() default {};

    /**
     * The bean name of the {@link org.springframework.rocket.core.RocketTemplate}
     * <p>
     * If not specified, the default rocket template is used.
     * @return the rocket template bean name.
     */
    String rocketTemplate() default RocketSupportBeanNames.DEFAULT_ROCKET_TEMPLATE_BEAN_NAME;

}
