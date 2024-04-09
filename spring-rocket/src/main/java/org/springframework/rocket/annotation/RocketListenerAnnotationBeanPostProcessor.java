package org.springframework.rocket.annotation;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.rocket.config.GenericListenerEndpointRegistrar;
import org.springframework.rocket.config.MethodRocketListenerEndpoint;
import org.springframework.rocket.config.RocketListenerContainerFactory;
import org.springframework.rocket.config.RocketListenerEndpointRegistry;
import org.springframework.rocket.config.RocketSupportBeanNames;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@Slf4j
public class RocketListenerAnnotationBeanPostProcessor extends AbstractRocketAnnotationBeanExpressionResolver
        implements BeanPostProcessor, Ordered, InitializingBean, SmartInitializingSingleton {

    public static final String DEFAULT_ROCKET_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "rocketListenerContainerFactory";
    private static final String GENERATED_ID_PREFIX = "org.springframework.rocket.RocketListenerEndpointContainer#";

    @Setter
    private RocketListenerEndpointRegistry endpointRegistry;
    @Setter
    private String defaultContainerFactoryBeanName = DEFAULT_ROCKET_LISTENER_CONTAINER_FACTORY_BEAN_NAME;
    @Setter
    private Charset charset = StandardCharsets.UTF_8;
    private final GenericListenerEndpointRegistrar registrar = new GenericListenerEndpointRegistrar();
    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));
    private final AtomicInteger counter = new AtomicInteger();
    private final RocketHandlerMethodFactoryAdapter messageHandlerMethodFactory = new RocketHandlerMethodFactoryAdapter();
    private AnnotationEnhancer enhancer;

    /**
     * Set the {@link MessageHandlerMethodFactory} to use to configure the message
     * listener responsible to serve an endpoint detected by this processor.
     * <p>By default, {@link DefaultMessageHandlerMethodFactory} is used, and it
     * can be configured further to support additional method arguments
     * or to customize conversion and validation support. See
     * {@link DefaultMessageHandlerMethodFactory} Javadoc for more details.
     * @param messageHandlerMethodFactory the {@link MessageHandlerMethodFactory} instance.
     */
    public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
        this.messageHandlerMethodFactory.setHandlerMethodFactory(messageHandlerMethodFactory);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() {
        buildEnhancer();
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.registrar.setBeanFactory(this.beanFactory);
        this.beanFactory.getBeanProvider(RocketListenerConfigurer.class)
                .forEach(configurer -> configurer.configureRocketListeners(this.registrar));
        if (this.registrar.getEndpointRegistry() == null) {
            if (this.endpointRegistry == null) {
                Assert.state(this.beanFactory != null,
                        "BeanFactory must be set to find endpoint registry by bean name");
                this.endpointRegistry = this.beanFactory.getBean(
                        RocketSupportBeanNames.ROCKET_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                        RocketListenerEndpointRegistry.class);
            }
            this.registrar.setEndpointRegistry(this.endpointRegistry);
        }
        if (this.defaultContainerFactoryBeanName != null) {
            this.registrar.setContainerFactoryBeanName(this.defaultContainerFactoryBeanName);
        }

        // Set the custom handler method factory once resolved by the configurer
        MessageHandlerMethodFactory handlerMethodFactory = this.registrar.getMessageHandlerMethodFactory();
        if (handlerMethodFactory != null) {
            this.messageHandlerMethodFactory.setHandlerMethodFactory(handlerMethodFactory);
        }
        else {
            addFormatters(this.messageHandlerMethodFactory.defaultFormattingConversionService);
        }

        // Actually register all listeners
        this.registrar.afterPropertiesSet();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Map<Method, Set<RocketListener>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<Set<RocketListener>>) method -> {
                        Set<RocketListener> listenerMethods = findListenerAnnotations(method);
                        return (!listenerMethods.isEmpty() ? listenerMethods : null);
                    });

//            Collection<RocketListener> classLevelListeners = findListenerAnnotations(targetClass);
//            final boolean hasClassLevelListeners = !classLevelListeners.isEmpty();
//            final List<Method> multiMethods = new ArrayList<>();
//            if (hasClassLevelListeners) {
//                Set<Method> methodsWithHandler = MethodIntrospector.selectMethods(targetClass,
//                        (ReflectionUtils.MethodFilter) method ->
//                                AnnotationUtils.findAnnotation(method, RocketListener.class) != null);
//                multiMethods.addAll(methodsWithHandler);
//            }
//            if (annotatedMethods.isEmpty() && !hasClassLevelListeners) {
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.add(bean.getClass());
                log.trace("No @{} annotations found on bean type: {}", RocketListener.class.getSimpleName(), bean.getClass());
            }
            else {
                // Non-empty set of methods
                for (Map.Entry<Method, Set<RocketListener>> entry : annotatedMethods.entrySet()) {
                    Method method = entry.getKey();
                    for (RocketListener listener : entry.getValue()) {
                        processRocketListener(listener, method, bean, beanName);
                    }
                }
                log.debug("{} @{} methods processed on bean '{}': {}", annotatedMethods.size(), RocketListener.class.getSimpleName(), beanName, annotatedMethods);
            }
//            if (hasClassLevelListeners) {
//                processMultiMethodListeners(classLevelListeners, multiMethods, bean, beanName);
//            }
        }
        return bean;
    }

    protected void processRocketListener(RocketListener rocketListener, Method method, Object bean, String beanName) {
        Method methodToUse = checkProxy(method, bean);
        MethodRocketListenerEndpoint endpoint = new MethodRocketListenerEndpoint();
        endpoint.setMethod(methodToUse);

        processListener(endpoint, rocketListener, bean, beanName);
    }

    protected void processListener(MethodRocketListenerEndpoint endpoint, RocketListener rocketListener, Object bean, String beanName) {
        processRocketListenerAnnotation(endpoint, rocketListener, bean);
        RocketListenerContainerFactory listenerContainerFactory = resolveContainerFactory(rocketListener, resolve(rocketListener.containerFactory()), beanName);

        this.registrar.registerEndpoint(endpoint, listenerContainerFactory);
    }

    private Method checkProxy(Method methodArg, Object bean) {
        Method method = methodArg;
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                // Found a @RocketListener method on the target class for this JDK proxy ->
                // is it also present on the proxy itself?
                method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
                for (var proxiedInterface : proxiedInterfaces) {
                    try {
                        method = proxiedInterface.getMethod(method.getName(), method.getParameterTypes());
                        break;
                    }
                    catch (@SuppressWarnings("unused") NoSuchMethodException noMethod) {
                        // NOSONAR
                    }
                }
            }
            catch (SecurityException ex) {
                ReflectionUtils.handleReflectionException(ex);
            }
            catch (NoSuchMethodException ex) {
                throw new IllegalStateException(String.format(
                        """
                            @RocketListener method '%s' found on bean target class '%s',\s
                            but not found in any interface(s) for bean JDK proxy. Either\s
                            pull the method up to an interface or switch to subclass (CGLIB)\s
                            proxies by setting proxy-target-class/proxyTargetClass\s
                            attribute to 'true'
                        """,
                        method.getName(), method.getDeclaringClass().getSimpleName()), ex);
            }
        }
        return method;
    }

    private RocketListenerContainerFactory resolveContainerFactory(RocketListener rocketListener, Object factoryTarget, String beanName) {
        String containerFactory = rocketListener.containerFactory();
        if (!StringUtils.hasText(containerFactory)) {
            return null;
        }

        Object resolved = resolveExpression(containerFactory);
        if (resolved instanceof RocketListenerContainerFactory listenerContainerFactory) {
            return listenerContainerFactory;
        }

        RocketListenerContainerFactory factory = null;
        String containerFactoryBeanName = resolveExpressionAsString(containerFactory, "containerFactory");
        if (StringUtils.hasText(containerFactoryBeanName)) {
            assertBeanFactory();
            try {
                factory = this.beanFactory.getBean(containerFactoryBeanName, RocketListenerContainerFactory.class);
            }
            catch (NoSuchBeanDefinitionException ex) {
                throw new BeanInitializationException(String.format(
                        """
                            Could not register rocket listener endpoint on [%s] for bean %s,\s
                            no '%s' with id '%s' was found in the application context
                        """,
                        factoryTarget, beanName, RocketListenerContainerFactory.class.getSimpleName(), containerFactoryBeanName), ex);
            }
        }
        return factory;
    }

    private void processRocketListenerAnnotation(MethodRocketListenerEndpoint endpoint, RocketListener rocketListener, Object bean) {
        endpoint.setBean(bean);
        endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory);
        // subscription
        endpoint.setTopic(resolveExpressionAsString(rocketListener.topic(), "topic"));
        String filterExpression = resolveExpressionAsString(rocketListener.filterExpression(), "filterExpression");
        if (StringUtils.hasText(filterExpression)) {
            endpoint.setFilterExpression(filterExpression);
        }
        if (rocketListener.filterExpressionType() != null) {
            endpoint.setFilterExpressionType(rocketListener.filterExpressionType().name());
        }

        endpoint.setId(getEndpointId(rocketListener));
        endpoint.setGroupId(resolveExpressionAsString(rocketListener.groupId(), "groupId"));

        String concurrency = rocketListener.concurrency();
        if (StringUtils.hasText(concurrency)) {
            endpoint.setConcurrency(resolveExpressionAsBoolean(concurrency, "concurrency"));
        }
        String autoStartup = rocketListener.autoStartup();
        if (StringUtils.hasText(autoStartup)) {
            endpoint.setAutoStartup(resolveExpressionAsBoolean(autoStartup, "autoStartup"));
        }

        resolveRocketProperties(endpoint, rocketListener.properties());
        endpoint.setBatchListener(rocketListener.batch());
//        endpoint.setAckMode(rocketListener.ackMode());
        endpoint.setBeanFactory(this.beanFactory);
    }

    private void addFormatters(FormatterRegistry registry) {
        this.beanFactory.getBeanProvider(Converter.class).forEach(registry::addConverter);
        this.beanFactory.getBeanProvider(ConverterFactory.class).forEach(registry::addConverterFactory);
        this.beanFactory.getBeanProvider(GenericConverter.class).forEach(registry::addConverter);
        this.beanFactory.getBeanProvider(Formatter.class).forEach(registry::addFormatter);
    }

    private void resolveRocketProperties(MethodRocketListenerEndpoint endpoint, String[] propertyStrings) {
        if (propertyStrings.length > 0) {
            endpoint.setConsumerProperties(resolveProperties(propertyStrings));
        }
    }

    private String getEndpointId(RocketListener rocketListener) {
        if (StringUtils.hasText(rocketListener.id())) {
            return resolveExpressionAsString(rocketListener.id(), "id");
        }
        else {
            return GENERATED_ID_PREFIX + this.counter.getAndIncrement();
        }
    }

    /**
     * AnnotationUtils.getRepeatableAnnotations does not look at interfaces
     * @param clazz class with {@link RocketListener} annotation
     */
    private Collection<RocketListener> findListenerAnnotations(Class<?> clazz) {
        Set<RocketListener> listeners = new HashSet<>();
        RocketListener ann = AnnotatedElementUtils.findMergedAnnotation(clazz, RocketListener.class);
        if (ann != null) {
            ann = enhance(clazz, ann);
            listeners.add(ann);
        }
        RocketListeners anns = AnnotationUtils.findAnnotation(clazz, RocketListeners.class);
        if (anns != null) {
            listeners.addAll(Arrays.stream(anns.value())
                    .map(anno -> enhance(clazz, anno))
                    .toList());
        }
        return listeners;
    }


    /**
     * AnnotationUtils.getRepeatableAnnotations does not look at interfaces
     * @param method method with {@link RocketListener} annotation
     */
    private Set<RocketListener> findListenerAnnotations(Method method) {
        Set<RocketListener> listeners = new HashSet<>();
        RocketListener ann = AnnotatedElementUtils.findMergedAnnotation(method, RocketListener.class);
        if (ann != null) {
            ann = enhance(method, ann);
            listeners.add(ann);
        }
        RocketListeners anns = AnnotationUtils.findAnnotation(method, RocketListeners.class);
        if (anns != null) {
            listeners.addAll(Arrays.stream(anns.value())
                    .map(anno -> enhance(method, anno))
                    .toList());
        }
        return listeners;
    }

    private RocketListener enhance(AnnotatedElement element, RocketListener ann) {
        if (this.enhancer == null) {
            return ann;
        }
        else {
            return AnnotationUtils.synthesizeAnnotation(
                    this.enhancer.apply(AnnotationUtils.getAnnotationAttributes(ann), element), RocketListener.class, null);
        }
    }

    protected class RocketHandlerMethodFactoryAdapter implements MessageHandlerMethodFactory {

        private final DefaultFormattingConversionService defaultFormattingConversionService = new DefaultFormattingConversionService();

        @Setter
        private MessageHandlerMethodFactory handlerMethodFactory;

        @Override
        public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
            return getHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
        }

        private MessageHandlerMethodFactory getHandlerMethodFactory() {
            if (this.handlerMethodFactory == null) {
                this.handlerMethodFactory = createDefaultMessageHandlerMethodFactory();
            }
            return this.handlerMethodFactory;
        }

        private MessageHandlerMethodFactory createDefaultMessageHandlerMethodFactory() {
            DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
            Validator validator = RocketListenerAnnotationBeanPostProcessor.this.registrar.getValidator();
            if (validator != null) {
                defaultFactory.setValidator(validator);
            }
            defaultFactory.setBeanFactory(RocketListenerAnnotationBeanPostProcessor.this.beanFactory); // NOSONAR
            this.defaultFormattingConversionService
                    .addConverter(new BytesToStringConverter(RocketListenerAnnotationBeanPostProcessor.this.charset));
            this.defaultFormattingConversionService.addConverter(new BytesToNumberConverter());
            defaultFactory.setConversionService(this.defaultFormattingConversionService);
            GenericMessageConverter messageConverter = new GenericMessageConverter(this.defaultFormattingConversionService);
            defaultFactory.setMessageConverter(messageConverter);

            List<HandlerMethodArgumentResolver> customArgumentsResolver =
                    new ArrayList<>(RocketListenerAnnotationBeanPostProcessor.this.registrar.getCustomMethodArgumentResolvers());
            defaultFactory.setCustomArgumentResolvers(customArgumentsResolver);

            defaultFactory.afterPropertiesSet();

            return defaultFactory;
        }
    }

    private record BytesToStringConverter(Charset charset) implements Converter<byte[], String> {

        @Override
        public String convert(byte[] source) {
            return new String(source, this.charset);
        }

    }

    private record BytesToNumberConverter() implements ConditionalGenericConverter {

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            HashSet<ConvertiblePair> pairs = new HashSet<>();
            pairs.add(new ConvertiblePair(byte[].class, long.class));
            pairs.add(new ConvertiblePair(byte[].class, int.class));
            pairs.add(new ConvertiblePair(byte[].class, short.class));
            pairs.add(new ConvertiblePair(byte[].class, byte.class));
            pairs.add(new ConvertiblePair(byte[].class, Long.class));
            pairs.add(new ConvertiblePair(byte[].class, Integer.class));
            pairs.add(new ConvertiblePair(byte[].class, Short.class));
            pairs.add(new ConvertiblePair(byte[].class, Byte.class));
            return pairs;
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            byte[] bytes = (byte[]) source;
            if (targetType.getType().equals(long.class) || targetType.getType().equals(Long.class)) {
                Assert.state(bytes.length >= 8, "At least 8 bytes needed to convert a byte[] to a long"); // NOSONAR
                return ByteBuffer.wrap(bytes).getLong();
            }
            else if (targetType.getType().equals(int.class) || targetType.getType().equals(Integer.class)) {
                Assert.state(bytes.length >= 4, "At least 4 bytes needed to convert a byte[] to an integer"); // NOSONAR
                return ByteBuffer.wrap(bytes).getInt();
            }
            else if (targetType.getType().equals(short.class) || targetType.getType().equals(Short.class)) {
                Assert.state(bytes.length >= 2, "At least 2 bytes needed to convert a byte[] to a short");
                return ByteBuffer.wrap(bytes).getShort();
            }
            else if (targetType.getType().equals(byte.class) || targetType.getType().equals(Byte.class)) {
                Assert.state(bytes.length >= 1, "At least 1 byte needed to convert a byte[] to a byte");
                return ByteBuffer.wrap(bytes).get();
            }
            return null;
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (sourceType.getType().equals(byte[].class)) {
                Class<?> target = targetType.getType();
                return target.equals(long.class) || target.equals(int.class) || target.equals(short.class) // NOSONAR
                        || target.equals(byte.class) || target.equals(Long.class) || target.equals(Integer.class)
                        || target.equals(Short.class) || target.equals(Byte.class);
            }
            return false;
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
