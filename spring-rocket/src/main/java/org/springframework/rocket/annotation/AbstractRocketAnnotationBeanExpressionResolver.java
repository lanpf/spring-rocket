package org.springframework.rocket.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;

@Slf4j
public abstract class AbstractRocketAnnotationBeanExpressionResolver implements ApplicationContextAware {

    protected BeanFactory beanFactory;

    protected ApplicationContext applicationContext;

    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    protected BeanExpressionContext expressionContext;

    protected void assertBeanFactory() {
        Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
    }

    /**
     * Making a {@link BeanFactory} available is optional; if not set,
     * {@link RocketListenerConfigurer} beans won't get autodetected and an
     * {@link RocketListenerAnnotationBeanPostProcessor#setEndpointRegistry endpoint registry} has to be explicitly configured.
     * @param beanFactory the {@link BeanFactory} to be used.
     */
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory configurable) {
            this.resolver = configurable.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(configurable, null);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (applicationContext instanceof ConfigurableApplicationContext configurable) {
            setBeanFactory(configurable.getBeanFactory());
        }
        else {
            setBeanFactory(applicationContext);
        }
    }

    /**
     * Resolve the specified value if possible.
     * @param value the value to resolve
     * @return the resolved value
     * @see ConfigurableBeanFactory#resolveEmbeddedValue
     */
    protected String resolve(String value) {
        if (this.beanFactory instanceof ConfigurableBeanFactory configurable) {
            return configurable.resolveEmbeddedValue(value);
        }
        return value;
    }

    protected Object resolveExpression(String value) {
        return this.resolver.evaluate(resolve(value), this.expressionContext);
    }

    protected String resolveExpressionAsString(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof String string) {
            return string;
        }
        else if (resolved != null) {
            throw new IllegalStateException(String.format(
                    "The [%s] must resolve to a String. Resolved to [%s] for [%s]",
                    attribute, resolved.getClass(), value));
        }
        return null;
    }

    protected Boolean resolveExpressionAsBoolean(String value, String attribute) {
        Object resolved = resolveExpression(value);

        if (resolved instanceof Boolean bool) {
            return bool;
        }
        else if (resolved instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        else if (resolved != null) {
            throw new IllegalStateException(String.format(
                    "The [%s] must resolve to a Boolean or a String that can be parsed as a Boolean. Resolved to [%s] for [%s]",
                    attribute, resolved.getClass(), value));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Properties resolveProperties(String[] propertyStrings) {
        Properties properties = new Properties();
        for (String property : propertyStrings) {
            Object value = resolveExpression(property);
            if (value instanceof String) {
                loadProperty(properties, property, value);
            } else if (value instanceof String[] values) {
                for (String prop : values) {
                    loadProperty(properties, prop, prop);
                }
            } else if (value instanceof Collection<?> values) {
                if (!values.isEmpty() && values.iterator().next() instanceof String) {
                    for (String prop : (Collection<String>) value) {
                        loadProperty(properties, prop, prop);
                    }
                }
            } else {
                throw new IllegalStateException(
                        "'properties' must resolve to a String, a String[] or Collection<String>");
            }
        }
        return properties;
    }

    private void loadProperty(Properties properties, String property, Object value) {
        try {
            properties.load(new StringReader((String) value));
        }
        catch (IOException e) {
            log.error(String.format("Failed to load property %s, continuing...", property), e);
        }
    }
}
