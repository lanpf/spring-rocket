package org.springframework.rocket.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public abstract class AbstractRocketAnnotationBeanExpressionResolver implements ApplicationContextAware {

    protected ApplicationContext applicationContext;
    protected BeanFactory beanFactory;
    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    protected BeanExpressionContext expressionContext;

    protected final ListenerScope listenerScope = new ListenerScope();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (applicationContext instanceof ConfigurableApplicationContext configurable) {
            setBeanFactory(configurable.getBeanFactory());
        } else {
            setBeanFactory(applicationContext);
        }
    }

    /**
     * Making a {@link BeanFactory} available is optional; if not set,
     * {@link RocketListenerConfigurer} beans won't get autodetected and an
     * {@link RocketListenerAnnotationBeanPostProcessor#setEndpointRegistry endpoint registry} has to be explicitly configured.
     *
     * @param beanFactory the {@link BeanFactory} to be used.
     */
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory configurable) {
            this.resolver = configurable.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(configurable, this.listenerScope);
        }
    }

    /**
     * Resolve the specified value if possible.
     *
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
        } catch (IOException e) {
            log.error(String.format("Failed to load property %s, continuing...", property), e);
        }
    }

    protected Object resolveExpression(String value) {
        return this.resolver.evaluate(resolve(value), this.expressionContext);
    }

    protected String resolveExpressionAsString(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof String string) {
            return string;
        } else if (resolved != null) {
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
        } else if (resolved instanceof String string) {
            return Boolean.parseBoolean(string);
        } else if (resolved != null) {
            throw new IllegalStateException(String.format(
                    "The [%s] must resolve to a Boolean or a String that can be parsed as a Boolean. Resolved to [%s] for [%s]",
                    attribute, resolved.getClass(), value));
        }
        return null;
    }

    protected Integer resolveExpressionAsInteger(String value, String attribute) {
        Object resolved = this.resolveExpression(value);
        if (resolved instanceof String string) {
            return Integer.parseInt(string);
        } else if (resolved instanceof Number number) {
            return number.intValue();
        } else if (resolved != null) {
            throw new IllegalStateException(String.format(
                    "The [%s] must resolve to an Number or a String that can be parsed as an Integer. Resolved to [%s] for [%s]",
                    attribute, resolved.getClass(), value));
        }
        return null;
    }

    public static class ListenerScope implements Scope {
        private final Map<String, Object> listeners = new HashMap<>();

        public ListenerScope() {
        }

        public void addListener(String key, Object bean) {
            this.listeners.put(key, bean);
        }

        public void removeListener(String key) {
            this.listeners.remove(key);
        }

        public Object get(String name, ObjectFactory<?> objectFactory) {
            return this.listeners.get(name);
        }

        public Object remove(String name) {
            return null;
        }

        public void registerDestructionCallback(String name, Runnable callback) {
        }

        public Object resolveContextualObject(String key) {
            return this.listeners.get(key);
        }

        public String getConversationId() {
            return null;
        }
    }
}
