package org.springframework.rocket.listener.adapter;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

public class HandlerAdapter {
    private final InvocableHandlerMethod invokerHandlerMethod;

    private final DelegatingInvocableHandler delegatingHandler;

    /**
     * Construct an instance with the provided method.
     * @param invokerHandlerMethod the method.
     */
    public HandlerAdapter(InvocableHandlerMethod invokerHandlerMethod) {
        this.invokerHandlerMethod = invokerHandlerMethod;
        this.delegatingHandler = null;
    }

    /**
     * Construct an instance with the provided delegating handler.
     * @param delegatingHandler the handler.
     */
    public HandlerAdapter(DelegatingInvocableHandler delegatingHandler) {
        this.invokerHandlerMethod = null;
        this.delegatingHandler = delegatingHandler;
    }

    public Object invoke(Message<?> message, Object... providedArgs) throws Exception { //NOSONAR
        if (this.invokerHandlerMethod != null) {
            // NOSONAR
            return this.invokerHandlerMethod.invoke(message, providedArgs);
        }
        else if (this.delegatingHandler.hasDefaultHandler()) {
            // Needed to avoid returning raw Message which matches Object
            Object[] args = new Object[providedArgs.length + 1];
            args[0] = message.getPayload();
            System.arraycopy(providedArgs, 0, args, 1, providedArgs.length);
            return this.delegatingHandler.invoke(message, args);
        }
        else {
            return this.delegatingHandler.invoke(message, providedArgs);
        }
    }

    public String getMethodAsString(Object payload) {
        if (this.invokerHandlerMethod != null) {
            return this.invokerHandlerMethod.getMethod().toGenericString();
        }
        else {
            return this.delegatingHandler.getMethodNameFor(payload);
        }
    }

    public Object getBean() {
        if (this.invokerHandlerMethod != null) {
            return this.invokerHandlerMethod.getBean();
        }
        else {
            return this.delegatingHandler.getBean();
        }
    }
}
