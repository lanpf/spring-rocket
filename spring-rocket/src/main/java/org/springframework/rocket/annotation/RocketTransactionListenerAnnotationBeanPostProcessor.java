package org.springframework.rocket.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.rocket.transaction.TransactionListener;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Slf4j
public class RocketTransactionListenerAnnotationBeanPostProcessor extends AbstractRocketAnnotationBeanExpressionResolver
        implements BeanPostProcessor, Ordered, InitializingBean {

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));
    private AnnotationEnhancer enhancer;

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() {
        buildEnhancer();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            RocketTransactionListener listener = findListenerAnnotation(targetClass);
            if (listener == null) {
                this.nonAnnotatedClasses.add(bean.getClass());
                log.trace("No @{} annotations found on bean type: {}", RocketTransactionListener.class.getSimpleName(), bean.getClass());
            } else {
                processRocketTransaction(listener, bean, beanName);
                log.debug("@{} classes processed on bean '{}': {}", RocketTransactionListener.class.getSimpleName(), beanName, listener);
            }
        }
        return bean;
    }

    protected void processRocketTransaction(RocketTransactionListener rocketTransactionListener, Object bean, String beanName) {
        Assert.isAssignable(TransactionListener.class, bean.getClass());

        RocketTemplate rocketTemplate = resolveRocketTemplate(rocketTransactionListener, resolve(rocketTransactionListener.rocketTemplate()), beanName);
        Assert.state(rocketTemplate != null, "No rocketTemplate found");

        String topic = resolveExpressionAsString(rocketTransactionListener.topic(), "topic");
        Assert.hasText(topic, "topic must not be null or empty");

        Properties properties = resolveProperties(rocketTransactionListener.properties());
        rocketTemplate.registerTransactionListener(topic, (TransactionListener) bean, PropertiesUtils.asMap(properties));
    }

    private RocketTemplate resolveRocketTemplate(RocketTransactionListener rocketTransactionListener, Object target, String beanName) {
        String rocketTemplate = rocketTransactionListener.rocketTemplate();
        if (!StringUtils.hasText(rocketTemplate)) {
            return null;
        }

        Object resolved = resolveExpression(rocketTemplate);
        if (resolved instanceof RocketTemplate template) {
            return template;
        }

        RocketTemplate template = null;
        String rocketTemplateBeanName = resolveExpressionAsString(rocketTemplate, "rocketTemplate");
        if (StringUtils.hasText(rocketTemplateBeanName)) {
            assertBeanFactory();
            try {
                template = this.beanFactory.getBean(rocketTemplateBeanName, RocketTemplate.class);
            }
            catch (NoSuchBeanDefinitionException ex) {
                throw new BeanInitializationException(String.format(
                        """
                            Could not register rocket transactional on [%s] for bean %s,\s
                            no '%s' with id '%s' was found in the application context
                        """,
                        target, beanName, RocketTemplate.class.getSimpleName(), rocketTemplateBeanName), ex);
            }
        }
        return template;
    }


    private void assertBeanFactory() {
        Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain rocket template by bean name");
    }

    /**
     * AnnotationUtils.getRepeatableAnnotations does not look at interfaces
     * @param clazz class with {@link RocketTransactionListener} annotation
     */
    private RocketTransactionListener findListenerAnnotation(Class<?> clazz) {
        RocketTransactionListener ann = AnnotatedElementUtils.findMergedAnnotation(clazz, RocketTransactionListener.class);
        if (ann != null) {
            ann = enhance(clazz, ann);
        }
        return ann;
    }


    private RocketTransactionListener enhance(AnnotatedElement element, RocketTransactionListener ann) {
        if (this.enhancer == null) {
            return ann;
        }
        else {
            return AnnotationUtils.synthesizeAnnotation(
                    this.enhancer.apply(AnnotationUtils.getAnnotationAttributes(ann), element), RocketTransactionListener.class, null);
        }
    }

    private void buildEnhancer() {
        if (this.applicationContext != null) {
            Map<String, AnnotationEnhancer> enhancersMap =
                    this.applicationContext.getBeansOfType(AnnotationEnhancer.class, false, false);
            if (!enhancersMap.isEmpty()) {
                List<AnnotationEnhancer> enhancers = enhancersMap.values()
                        .stream()
                        .sorted(new OrderComparator())
                        .toList();
                this.enhancer = (attrs, element) -> {
                    Map<String, Object> newAttrs = attrs;
                    for (var enhancer : enhancers) {
                        newAttrs = enhancer.apply(newAttrs, element);
                    }
                    return attrs;
                };
            }
        }
    }


    public interface AnnotationEnhancer extends BiFunction<Map<String, Object>, AnnotatedElement, Map<String, Object>> {

    }
}
