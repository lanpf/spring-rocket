package org.springframework.rocket.listener;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;

@Getter
@Setter
public abstract class AbstractContainer implements ApplicationEventPublisherAware, BeanNameAware,
        ApplicationContextAware, SmartLifecycle, DisposableBean {

    protected ApplicationEventPublisher applicationEventPublisher;
    /**
     * The container factory sets this to the listener id
     */
    protected String beanName;
    protected ApplicationContext applicationContext;
    protected int phase;
    protected boolean autoStartup = true;
    protected volatile boolean running = false;

    protected abstract void doStart();

    protected abstract void doStop();
}
