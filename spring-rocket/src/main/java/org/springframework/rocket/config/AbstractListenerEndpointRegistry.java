package org.springframework.rocket.config;

import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.rocket.listener.ListenerContainerRegistry;
import org.springframework.rocket.listener.MessageListenerContainer;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractListenerEndpointRegistry<C extends MessageListenerContainer, E extends ListenerEndpoint<C>>
        implements ListenerContainerRegistry, DisposableBean, SmartLifecycle,
        ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private final Class<? extends C> type;
    private final Map<String, C> unregisteredContainers = new ConcurrentHashMap<>();
    private final Map<String, C> listenerContainers = new ConcurrentHashMap<>();
    private final ReentrantLock containersLock = new ReentrantLock();
    private ConfigurableApplicationContext applicationContext;
    private int phase = C.DEFAULT_PHASE;
    private boolean contextRefreshed;
    /**
     *  By default, containers registered for endpoints after the context is refreshed
     *  are immediately started, regardless of their autoStartup property, to comply with
     *  the
     *  contract, where autoStartup is only considered during
     *  context initialization. Set to false to apply the autoStartup property, even for
     *  late endpoint binding. If this is called after the context is refreshed, it will
     *  apply to any endpoints registered after that call.
     *
     */
    @Setter
    private boolean alwaysStartAfterRefresh = true;
    private volatile boolean running;


    protected AbstractListenerEndpointRegistry(Class<? extends C> type) {
        this.type = type;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext configurable) {
            this.applicationContext = configurable;
        }
    }

    /**
     * Return the {@link MessageListenerContainer} with the specified id or
     * {@code null} if no such container exists.
     * @param id the id of the container
     * @return the container or {@code null} if no container with that id exists
     * @see ListenerEndpoint#getId()
     * @see #getListenerContainerIds()
     */
    @Override
    public C getListenerContainer(String id) {
        Assert.hasText(id, "Container identifier must not be empty");
        return this.listenerContainers.get(id);
    }


    /**
     * Return the ids of the managed {@link MessageListenerContainer} instance(s).
     * @return the ids.
     * @see #getListenerContainer(String)
     */
    @Override
    public Set<String> getListenerContainerIds() {
        return Collections.unmodifiableSet(this.listenerContainers.keySet());
    }

    /**
     * Return the managed {@link MessageListenerContainer} instance(s).
     * @return the managed {@link MessageListenerContainer} instance(s).
     * @see #getAllListenerContainers()
     */
    @Override
    public Collection<C> getListenerContainers() {
        return Collections.unmodifiableCollection(this.listenerContainers.values());
    }

    /**
     * Return all {@link MessageListenerContainer} instances including those managed by
     * this registry and those declared as beans in the application context.
     * Prototype-scoped containers will be included. Lazy beans that have not yet been
     * created will not be initialized by a call to this method.
     * @return the {@link MessageListenerContainer} instance(s).
     * @see #getListenerContainers()
     */
    @Override
    public Collection<C> getAllListenerContainers() {
        List<C> containers = new ArrayList<>(getListenerContainers());
        refreshContextContainers();
        containers.addAll(this.unregisteredContainers.values());
        return containers;
    }

    private void refreshContextContainers() {
        this.unregisteredContainers.clear();
        this.applicationContext.getBeansOfType(this.type, true, false).values()
                .forEach(container -> this.unregisteredContainers.put(container.getListenerId(), container));
    }

    public C getUnregisteredListenerContainer(String id) {
        C container = this.unregisteredContainers.get(id);
        if (container == null) {
            refreshContextContainers();
            return this.unregisteredContainers.get(id);
        }
        return null;
    }


    /**
     * Unregister the listener container with the provided id.
     * <p>
     * IMPORTANT: this method simply removes the container from the registry. It does NOT
     * call any {@link org.springframework.context.Lifecycle} or {@link DisposableBean}
     * methods; you need to call them before or after calling this method to shut down the
     * container.
     * @param id the id.
     * @return the container, if it was registered; null otherwise.
     */
    public C unregisterListenerContainer(String id) {
        return this.listenerContainers.remove(id);
    }

    public void registerListenerContainer(E endpoint, ListenerContainerFactory<? extends C, E> factory) {
        registerListenerContainer(endpoint, factory, false);
    }

    /**
     * Create a message listener container for the given {@link ListenerEndpoint}.
     * <p>This create the necessary infrastructure to honor that endpoint
     * with regards to its configuration.
     * <p>The {@code startImmediately} flag determines if the container should be
     * started immediately.
     * @param endpoint the endpoint to add.
     * @param factory the {@link ListenerContainerFactory} to use.
     * @param startImmediately start the container immediately if necessary
     * @see #getListenerContainers()
     * @see #getListenerContainer(String)
     */
    public void registerListenerContainer(E endpoint, ListenerContainerFactory<? extends C, E> factory, boolean startImmediately) {
        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.notNull(factory, "Factory must not be null");

        String id = endpoint.getId();
        Assert.hasText(id, "Endpoint id must not be empty");
        this.containersLock.lock();

        try {
            Assert.state(!this.listenerContainers.containsKey(id),
                    String.format("Another endpoint is already registered with id '%s'", id));
            C container = createListenerContainer(endpoint, factory);
            this.listenerContainers.put(id, container);
            if (startImmediately) {
                startIfNecessary(container);
            }
        }
        finally {
            this.containersLock.unlock();
        }
    }

    /**
     * Create and start a new {@link MessageListenerContainer} using the specified factory.
     * @param endpoint the endpoint to create a {@link MessageListenerContainer}.
     * @param factory the {@link ListenerContainerFactory} to use.
     * @return the {@link MessageListenerContainer}.
     */
    protected C createListenerContainer(E endpoint, ListenerContainerFactory<? extends C, E> factory) {
        C listenerContainer = factory.createListenerContainer(endpoint);

        if (listenerContainer instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
            }
            catch (Exception ex) {
                throw new BeanInitializationException("Failed to initialize message listener container", ex);
            }
        }

        int containerPhase = listenerContainer.getPhase();
        // a custom phase value
        if (listenerContainer.isAutoStartup() && containerPhase != C.DEFAULT_PHASE) {
            if (this.phase != C.DEFAULT_PHASE && this.phase != containerPhase) {
                throw new IllegalStateException(String.format(
                        "Encountered phase mismatch between container factory definitions: %s vs %s",
                        this.phase, containerPhase));
            }
            this.phase = listenerContainer.getPhase();
        }

        return listenerContainer;
    }

    @Override
    public void destroy() {
        for (C listenerContainer : getListenerContainers()) {
            listenerContainer.destroy();
        }
    }

    @Override
    public int getPhase() {
        return this.phase;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        for (C listenerContainer : getListenerContainers()) {
            startIfNecessary(listenerContainer);
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        for (C listenerContainer : getListenerContainers()) {
            listenerContainer.stop();
        }
    }

    @Override
    public void stop(Runnable callback) {
        this.running = false;
        Collection<C> listenerContainers = getListenerContainers();
        if (!listenerContainers.isEmpty()) {
            Runnable aggregatingCallback = new AggregatingCallback(new AtomicInteger(listenerContainers.size()), callback);
            for (C listenerContainer : listenerContainers) {
                if (listenerContainer.isRunning()) {
                    listenerContainer.stop(aggregatingCallback);
                } else {
                    aggregatingCallback.run();
                }
            }
        } else {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().equals(this.applicationContext)) {
            this.contextRefreshed = true;
        }
    }


    /**
     * Start the specified {@link MessageListenerContainer} if it should be started
     * on startup.
     * @param listenerContainer the listener container to start.
     * @see MessageListenerContainer#isAutoStartup()
     */
    private void startIfNecessary(MessageListenerContainer listenerContainer) {
        if ((this.contextRefreshed && this.alwaysStartAfterRefresh) || listenerContainer.isAutoStartup()) {
            listenerContainer.start();
        }
    }

    private record AggregatingCallback(AtomicInteger count, Runnable finishCallback) implements Runnable {

        @Override
        public void run() {
            if (this.count.decrementAndGet() <= 0) {
                this.finishCallback.run();
            }
        }

    }

}
