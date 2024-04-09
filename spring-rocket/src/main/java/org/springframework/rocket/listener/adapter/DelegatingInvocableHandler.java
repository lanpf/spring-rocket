package org.springframework.rocket.listener.adapter;

import lombok.Getter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.rocket.RocketException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class DelegatingInvocableHandler {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext("!{", "}");

    private final List<InvocableHandlerMethod> handlers;

    private final ConcurrentMap<Class<?>, InvocableHandlerMethod> cachedHandlers = new ConcurrentHashMap<>();

    private final ConcurrentMap<InvocableHandlerMethod, MethodParameter> payloadMethodParameters = new ConcurrentHashMap<>();

    private final InvocableHandlerMethod defaultHandler;

    private final Map<InvocableHandlerMethod, Expression> handlerSendTo = new ConcurrentHashMap<>();

    private final Map<InvocableHandlerMethod, Boolean> handlerReturnsMessage = new ConcurrentHashMap<>();

    private final Object bean;

    private final BeanExpressionResolver resolver;

    private final BeanExpressionContext beanExpressionContext;

    private final ConfigurableListableBeanFactory beanFactory;

    private final PayloadValidator validator;


    public DelegatingInvocableHandler(List<InvocableHandlerMethod> handlers,
                                      InvocableHandlerMethod defaultHandler, Object bean,
                                      BeanExpressionResolver beanExpressionResolver,
                                      BeanExpressionContext beanExpressionContext,
                                      BeanFactory beanFactory, Validator validator) {

        this.handlers = new ArrayList<>(handlers);
        this.defaultHandler = defaultHandler;
        this.bean = bean;
        this.resolver = beanExpressionResolver;
        this.beanExpressionContext = beanExpressionContext;
        this.beanFactory = beanFactory instanceof ConfigurableListableBeanFactory configurable ? configurable : null;
        this.validator = validator == null ? null : new PayloadValidator(validator);
    }

    /**
     * Invoke the method with the given message.
     * @param message the message.
     * @param providedArgs additional arguments.
     * @return the result of the invocation.
     * @throws Exception raised if no suitable argument resolver can be found,
     * or the method raised an exception.
     */
    public Object invoke(Message<?> message, Object... providedArgs) throws Exception { //NOSONAR
        Class<?> payloadClass = message.getPayload().getClass();
        InvocableHandlerMethod handler = getHandlerForPayload(payloadClass);
        if (this.validator != null && this.defaultHandler != null) {
            MethodParameter parameter = this.payloadMethodParameters.get(handler);
            if (parameter != null) {
                this.validator.validate(message, parameter, message.getPayload());
            }
        }
        Object result = handler.invoke(message, providedArgs);
        Expression replyTo = this.handlerSendTo.get(handler);
        return new InvocationResult(result, replyTo, this.handlerReturnsMessage.get(handler));
    }


    /**
     * Determine the {@link InvocableHandlerMethod} for the provided type.
     * @param payloadClass the payload class.
     * @return the handler.
     */
    protected InvocableHandlerMethod getHandlerForPayload(Class<?> payloadClass) {
        InvocableHandlerMethod handler = this.cachedHandlers.get(payloadClass);
        if (handler == null) {
            handler = findHandlerForPayload(payloadClass);
            if (handler == null) {
                ReflectionUtils.rethrowRuntimeException(
                        new NoSuchMethodException(String.format(
                                "No listener method found in %s for %s",
                                this.bean.getClass().getName(), payloadClass)));
            }
            setupReplyTo(handler);
            //NOSONAR
            this.cachedHandlers.putIfAbsent(payloadClass, handler);
        }
        return handler;
    }

    private void setupReplyTo(InvocableHandlerMethod handler) {

        Method method = handler.getMethod();
        String replyTo = extractSendTo(method.toString(), AnnotationUtils.getAnnotation(method, SendTo.class));
        if (replyTo == null) {
            Class<?> beanType = handler.getBeanType();
            replyTo = extractSendTo(beanType.getSimpleName(), AnnotationUtils.getAnnotation(beanType, SendTo.class));
        }
        if (replyTo != null) {
            this.handlerSendTo.put(handler, PARSER.parseExpression(replyTo, PARSER_CONTEXT));
        }
        this.handlerReturnsMessage.put(handler, returnTypeMessageOrCollectionOf(method));
    }

    /**
     * Return true if the method return type is {@link Message} or
     * {@code Collection<Message<?>>}.
     * @param method the method.
     * @return true if it returns message(s).
     */
    private boolean returnTypeMessageOrCollectionOf(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType.equals(Message.class)) {
            return true;
        }
        if (returnType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(Message.class)) {
                return true;
            }
            if (rawType.equals(Collection.class)) {
                Type collectionType = parameterizedType.getActualTypeArguments()[0];
                if (collectionType.equals(Message.class)) {
                    return true;
                }
                return collectionType instanceof ParameterizedType collectionParameterizedType
                        && collectionParameterizedType.getRawType().equals(Message.class);
            }
        }
        return false;

    }

    private String extractSendTo(String element, SendTo ann) {
        String replyTo = null;
        if (ann != null) {
            String[] destinations = ann.value();
            if (destinations.length > 1) {
                throw new IllegalStateException(String.format(
                        "Invalid @SendTo annotation on '%s' one destination must be set (got %s)",
                        element, Arrays.toString(destinations)));
            }
            replyTo = destinations.length == 1 ? destinations[0] : null;
            if (replyTo != null && this.beanFactory != null) {
                replyTo = this.beanFactory.resolveEmbeddedValue(replyTo);
                if (replyTo != null) {
                    replyTo = resolve(replyTo);
                }
            }
        }
        return replyTo;
    }

    private String resolve(String value) {
        if (this.resolver != null) {
            String resolvedValue = this.beanExpressionContext.getBeanFactory().resolveEmbeddedValue(value);
            Object newValue = this.resolver.evaluate(resolvedValue, this.beanExpressionContext);
            Assert.isInstanceOf(String.class, newValue, "Invalid @SendTo expression");
            return (String) newValue;
        }
        else {
            return value;
        }
    }

    protected InvocableHandlerMethod findHandlerForPayload(Class<?> payloadClass) {
        InvocableHandlerMethod result = null;
        for (InvocableHandlerMethod handler : this.handlers) {
            if (matchHandlerMethod(payloadClass, handler)) {
                if (result != null) {
                    boolean resultIsDefault = result.equals(this.defaultHandler);
                    if (!handler.equals(this.defaultHandler) && !resultIsDefault) {
                        throw new RocketException(String.format(
                                "Ambiguous methods for payload type: %s: %s and %s",
                                payloadClass, result.getMethod().getName(), handler.getMethod().getName())
                        );
                    }
                    if (!resultIsDefault) {
                        continue; // otherwise replace the result with the actual match
                    }
                }
                result = handler;
            }
        }
        return result != null ? result : this.defaultHandler;
    }

    protected boolean matchHandlerMethod(Class<?> payloadClass, InvocableHandlerMethod handler) {
        Method method = handler.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        // Single param; no annotation or not @Header
        if (parameterAnnotations.length == 1) {
            MethodParameter methodParameter = new MethodParameter(method, 0);
            if ((methodParameter.getParameterAnnotations().length == 0
                    || !methodParameter.hasParameterAnnotation(Header.class))
                    && methodParameter.getParameterType().isAssignableFrom(payloadClass)) {
                if (this.validator != null) {
                    this.payloadMethodParameters.put(handler, methodParameter);
                }
                return true;
            }
        }

        MethodParameter foundCandidate = findCandidate(payloadClass, method, parameterAnnotations);
        if (foundCandidate != null && this.validator != null) {
            this.payloadMethodParameters.put(handler, foundCandidate);
        }
        return foundCandidate != null;
    }

    private MethodParameter findCandidate(Class<?> payloadClass, Method method,
                                          Annotation[][] parameterAnnotations) {
        MethodParameter foundCandidate = null;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            if ((methodParameter.getParameterAnnotations().length == 0
                    || !methodParameter.hasParameterAnnotation(Header.class))
                    && methodParameter.getParameterType().isAssignableFrom(payloadClass)) {
                if (foundCandidate != null) {
                    throw new RocketException("Ambiguous payload parameter for " + method.toGenericString());
                }
                foundCandidate = methodParameter;
            }
        }
        return foundCandidate;
    }

    /**
     * Return a string representation of the method that will be invoked for this payload.
     * @param payload the payload.
     * @return the method name.
     */
    public String getMethodNameFor(Object payload) {
        InvocableHandlerMethod handlerForPayload = getHandlerForPayload(payload.getClass());
        return handlerForPayload == null ? "no match" : handlerForPayload.getMethod().toGenericString(); //NOSONAR
    }

    public boolean hasDefaultHandler() {
        return this.defaultHandler != null;
    }

    private static final class PayloadValidator extends PayloadMethodArgumentResolver {

        PayloadValidator(Validator validator) {
            super(new MessageConverter() { // Required but never used

                @Override
                @Nullable
                public Message<?> toMessage(Object payload, @Nullable
                MessageHeaders headers) {
                    return null;
                }

                @Override
                @Nullable
                public Object fromMessage(Message<?> message, Class<?> targetClass) {
                    return null;
                }

            }, validator);
        }

        @Override
        public void validate(Message<?> message, MethodParameter parameter, Object target) { // NOSONAR - public
            super.validate(message, parameter, target);
        }

    }
}
