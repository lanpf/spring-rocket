package org.springframework.rocket.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Getter
@Setter
public non-sealed abstract class AbstractRocketMessageListenerContainer extends AbstractContainer
        implements RocketMessageListenerContainer {

    protected final ContainerProperties containerProperties;

    protected final ReentrantLock lifecycleLock = new ReentrantLock();

    protected volatile boolean paused;

    @Override
    public String getGroupId() {
        return this.containerProperties.getGroupId();
    }

    @Override
    public String getListenerId() {
        return this.beanName;
    }

    @Override
    public void setupMessageListener(Object messageListener) {
        this.containerProperties.setMessageListener(messageListener);
    }


    @Override
    public void start() {
        this.lifecycleLock.lock();
        try {
            if (!isRunning()) {
                Assert.state(this.containerProperties.getMessageListener() instanceof RocketMessageListener
                                || this.containerProperties.getMessageListener() instanceof BatchRocketMessageListener,
                        String.format("A %s or %s implementation must be provided", RocketMessageListener.class.getName(), BatchRocketMessageListener.class.getName()));
                doStart();
            }
        }
        finally {
            this.lifecycleLock.unlock();
        }
    }


    @Override
    public void stop() {
        this.lifecycleLock.lock();
        try {
            if (isRunning()) {
                doStop();
            }
        }
        finally {
            this.lifecycleLock.unlock();
        }
    }

    @Override
    public void pause() {
        this.lifecycleLock.lock();
        try {
            doPause();
        }
        finally {
            this.lifecycleLock.unlock();
        }
    }

    @Override
    public void resume() {
        this.lifecycleLock.lock();
        try {
            doResume();
        }
        finally {
            this.lifecycleLock.unlock();
        }
    }

    protected abstract void doPause();

    protected abstract void doResume();
}