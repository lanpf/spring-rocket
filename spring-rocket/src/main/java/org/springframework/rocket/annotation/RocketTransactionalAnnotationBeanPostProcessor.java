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
import org.springframework.rocket.transaction.RocketTransactionListener;
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
public class RocketTransactionalAnnotationBeanPostProcessor extends AbstractRocketAnnotationBeanExpressionResolver
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

            RocketTransactional listener = findAnnotation(targetClass);
            if (listener == null) {
                this.nonAnnotatedClasses.add(bean.getClass());
                log.trace("No @{} annotations found on bean type: {}", RocketTransactional.class.getSimpleName(), bean.getClass());
            } else {
                processRocketTransactional(listener, bean, beanName);
                log.debug("@{} classes processed on bean '{}': {}", RocketTransactional.class.getSimpleName(), beanName, listener);
            }
        }
        return bean;
    }

    protected void processRocketTransactional(RocketTransactional rocketTransactional, Object bean, String beanName) {
        Assert.isAssignable(RocketTransactionListener.class, bean.getClass());

        RocketTemplate rocketTemplate = resolveRocketTemplate(rocketTransactional, resolve(rocketTransactional.rocketTemplate()), beanName);
        Assert.state(rocketTemplate != null, "No rocketTemplate found");

        String topic = resolveExpressionAsString(rocketTransactional.topic(), "topic");
        Assert.hasText(topic, "topic must not be null or empty");

        Properties properties = resolveProperties(rocketTransactional.properties());
        rocketTemplate.registerTransactional(topic, (RocketTransactionListener) bean, PropertiesUtils.asMap(properties));
    }

    private RocketTemplate resolveRocketTemplate(RocketTransactional rocketTransactional, Object target, String beanName) {
        String rocketTemplate = rocketTransactional.rocketTemplate();
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

    /**
     * AnnotationUtils.getRepeatableAnnotations does not look at interfaces
     * @param clazz class with {@link RocketTransactional} annotation
     */
    private RocketTransactional findAnnotation(Class<?> clazz) {
        RocketTransactional ann = AnnotatedElementUtils.findMergedAnnotation(clazz, RocketTransactional.class);
        if (ann != null) {
            ann = enhance(clazz, ann);
        }
        return ann;
    }


    private RocketTransactional enhance(AnnotatedElement element, RocketTransactional ann) {
        if (this.enhancer == null) {
            return ann;
        }
        else {
            return AnnotationUtils.synthesizeAnnotation(
                    this.enhancer.apply(AnnotationUtils.getAnnotationAttributes(ann), element), RocketTransactional.class, null);
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
