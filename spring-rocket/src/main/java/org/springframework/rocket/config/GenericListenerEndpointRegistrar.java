package org.springframework.rocket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericListenerEndpointRegistrar implements BeanFactoryAware, InitializingBean {

    private final List<GenericListenerEndpointDescriptor> endpointDescriptors = new ArrayList<>();
    private final ReentrantLock endpointDescriptorsLock = new ReentrantLock();
    private List<HandlerMethodArgumentResolver> customMethodArgumentResolvers = new ArrayList<>();

    @Getter
    @Setter
    private AbstractListenerEndpointRegistry endpointRegistry;

    /**
     * Set the {@link ListenerContainerFactory} to use in case a {@link ListenerEndpoint}
     * is registered with a {@code null} container factory.
     * <p>Alternatively, the bean name of the {@link ListenerContainerFactory} to use
     * can be specified for a lazy lookup, see {@link #setContainerFactoryBeanName}.
     */
    @Setter
    private ListenerContainerFactory<?, ?> containerFactory;

    @Setter
    private String containerFactoryBeanName;

    @Setter
    private BeanFactory beanFactory;

    private boolean startImmediately;

    @Getter
    private MessageHandlerMethodFactory messageHandlerMethodFactory;

    @Getter
    private Validator validator;


    public List<HandlerMethodArgumentResolver> getCustomMethodArgumentResolvers() {
        return Collections.unmodifiableList(this.customMethodArgumentResolvers);
    }

    /**
     * Add custom methods arguments resolvers to
     * {@link org.springframework.rocket.annotation.RocketListenerAnnotationBeanPostProcessor}
     * Default empty list.
     * @param methodArgumentResolvers the methodArgumentResolvers to assign.
     */
    public void setCustomMethodArgumentResolvers(HandlerMethodArgumentResolver... methodArgumentResolvers) {
        this.customMethodArgumentResolvers = Arrays.asList(methodArgumentResolvers);
    }

    /**
     * Set the {@link MessageHandlerMethodFactory} to use to configure the message
     * listener responsible to serve an endpoint detected by this processor.
     * <p>By default,
     * {@link org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory}
     * is used and it can be configured further to support additional method arguments
     * or to customize conversion and validation support. See
     * {@link org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory}
     * javadoc for more details.
     * @param messageHandlerMethodFactory the {@link MessageHandlerMethodFactory} instance.
     */
    public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
        Assert.isNull(this.validator, "A validator cannot be provided with a custom message handler factory");
        this.messageHandlerMethodFactory = messageHandlerMethodFactory;
    }

    public void setValidator(Validator validator) {
        Assert.isNull(this.messageHandlerMethodFactory, "A validator cannot be provided with a custom message handler factory");
        this.validator = validator;
    }

    @Override
    public void afterPropertiesSet() {
        registerAllEndpoints();
    }
    protected void registerAllEndpoints() {
        this.endpointDescriptorsLock.lock();
        try {
            for (GenericListenerEndpointDescriptor descriptor : this.endpointDescriptors) {
                ListenerContainerFactory<?, ?> factory = resolveContainerFactory(descriptor);
                this.endpointRegistry.registerListenerContainer(descriptor.endpoint, factory);
            }
            // trigger immediate startup
            this.startImmediately = true;
        }
        finally {
            this.endpointDescriptorsLock.unlock();
        }
    }

    private ListenerContainerFactory<?, ?> resolveContainerFactory(GenericListenerEndpointDescriptor descriptor) {
        if (descriptor.containerFactory != null) {
            return descriptor.containerFactory;
        }
        else if (this.containerFactory != null) {
            return this.containerFactory;
        }
        else if (this.containerFactoryBeanName != null) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            this.containerFactory = this.beanFactory.getBean(this.containerFactoryBeanName, RocketListenerContainerFactory.class);
            // Consider changing this if live change of the factory is required
            return this.containerFactory;
        }
        else {
            throw new IllegalStateException(String.format(
                    """
                        Could not resolve the %s to use for [%s]\s
                        no factory was given and no default is set.
                    """,
                    ListenerContainerFactory.class.getSimpleName(), descriptor.endpoint));
        }
    }

    /**
     * Register a new {@link ListenerEndpoint} alongside the
     * {@link ListenerContainerFactory} to use to create the underlying container.
     * <p>The {@code factory} may be {@code null} if the default factory has to be
     * used for that endpoint.
     * @param endpoint the {@link ListenerEndpoint} instance to register.
     * @param factory the {@link ListenerContainerFactory} to use.
     */
    public void registerEndpoint(ListenerEndpoint<?> endpoint, ListenerContainerFactory<?, ?> factory) {
        Assert.notNull(endpoint, "Endpoint must be set");
        Assert.hasText(endpoint.getId(), "Endpoint id must be set");
        // Factory may be null, we defer the resolution right before actually creating the container
        GenericListenerEndpointDescriptor descriptor = new GenericListenerEndpointDescriptor(endpoint, factory);
        this.endpointDescriptorsLock.lock();
        try {
            // Register and start immediately
            if (this.startImmediately) {
                this.endpointRegistry.registerListenerContainer(descriptor.endpoint, resolveContainerFactory(descriptor), true);
            }
            else {
                this.endpointDescriptors.add(descriptor);
            }
        }
        finally {
            this.endpointDescriptorsLock.unlock();
        }
    }


    private record GenericListenerEndpointDescriptor(ListenerEndpoint<?> endpoint, ListenerContainerFactory<?, ?> containerFactory) {

    }

}
