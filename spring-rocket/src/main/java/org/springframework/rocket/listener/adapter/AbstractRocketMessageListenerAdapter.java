package org.springframework.rocket.listener.adapter;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.support.RocketHeaderUtils;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.support.converter.DefaultMessagingMessageConverter;
import org.springframework.rocket.support.converter.MessagingMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Getter
public abstract class AbstractRocketMessageListenerAdapter {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext("!{", "}");
    private final Object bean;
    private final Type inferredType;
    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
    @Setter
    private HandlerAdapter handlerMethod;

    private boolean headerFound = false;
    private boolean simpleExtraction = false;
    private boolean isRocketMessageList;
    private boolean isRocketMessages;
    private boolean isSpringMessageList;
    private boolean isSpringMessage;

    private MessagingMessageConverter messageConverter = new DefaultMessagingMessageConverter();
    private boolean converterSet;
    @Setter
    private Type fallbackType = Object.class;
    private Expression replyTopicExpression;
    @Setter
    private RocketTemplate replyTemplate;
    private boolean messageReturnType;

    public AbstractRocketMessageListenerAdapter(Object bean, Method method) {
        this.bean = bean;
        this.inferredType = determineInferredType(method);
    }

    public void setMessageConverter(MessagingMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
        this.converterSet = true;
    }

    public void setMessagingConverter(SmartMessageConverter smartMessageConverter) {
        Assert.isTrue(!this.converterSet,
                """
                Cannot set the SmartMessageConverter when setting the messageConverter,\s
                add the SmartConverter to the message converter instead
                """);
        ((DefaultMessagingMessageConverter) this.messageConverter).setMessagingConverter(smartMessageConverter);
    }

    protected Type getType() {
        return this.inferredType == null ? this.fallbackType : this.inferredType;
    }

    public void setReplyTopic(String replyTopic) {
        if (!StringUtils.hasText(replyTopic)) {
            replyTopic = PARSER_CONTEXT.getExpressionPrefix() +
                    "source.headers['" + RocketHeaders.REPLY_TOPIC + "']"
                    + PARSER_CONTEXT.getExpressionSuffix();
        }
        if (replyTopic.contains(PARSER_CONTEXT.getExpressionPrefix())) {
            this.replyTopicExpression = PARSER.parseExpression(replyTopic, PARSER_CONTEXT);
        }
        else {
            this.replyTopicExpression = new LiteralExpression(replyTopic);
        }
    }

    public void setBeanResolver(BeanResolver beanResolver) {
        this.evaluationContext.setBeanResolver(beanResolver);
        this.evaluationContext.setTypeConverter(new StandardTypeConverter());
        this.evaluationContext.addPropertyAccessor(new MapAccessor());
    }

    protected org.springframework.messaging.Message<?> toMessagingMessage(Message rocketMessage) {
        return getMessageConverter().toMessage(rocketMessage, getType());
    }

    protected final Object invokeHandler(org.springframework.messaging.Message<?> message, Object... providedArgs) {
        try {
            return this.handlerMethod.invoke(message, providedArgs);
        }
        catch (Exception e) {
            throw new MessageConversionException("Cannot handle message", e);
        }
    }

    /**
     * Handle the given result object returned from the listener method, sending a
     * response message to the SendTo topic.
     * @param resultArg the result object to handle (never <code>null</code>)
     * @param request the original request message
     * @param source the source data for the method invocation - e.g.
     * {@code org.springframework.messaging.Message<?>}; may be null
     */
    protected void handleResult(Object resultArg, Object request, Object source) {
        log.debug("Listener method returned result [{}] - generating response message for it", resultArg);
        Object result = resultArg;
        boolean messageReturnType;
        if (resultArg instanceof InvocationResult invocationResult) {
            result = invocationResult.result();
            messageReturnType = invocationResult.messageReturnType();
        } else {
            messageReturnType = this.messageReturnType;
        }
        Assert.state(this.replyTemplate != null, "a RocketTemplate is required to support replies");
        sendResponse(result, request, source, messageReturnType);
    }

    @SneakyThrows
    protected void sendResponse(Object result, Object request, Object source, boolean returnTypeMessage) {
        String replyTopic = evaluateReplyTopic(request, source, result);
        Assert.state(replyTopic != null, "a replyTopic is required to support replies");

        Supplier<Map<String, Object>> replyHeaderSupplier = null;
        if (request instanceof Message requestMessage) {
            replyHeaderSupplier = () -> {
                Map<String, Object> headers = new HashMap<>(64);
                RocketHeaderUtils.REPLY_HEADER_SET.accept(headers, requestMessage);
                return headers;
            };
        }

        BiConsumer<SendResult, Throwable> sendConsumer = (sendResult, e) -> {
            if (e == null) {
                if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                    log.error("replyTemplate reply message failed: {}", sendResult.getSendStatus());
                } else {
                    log.info("replyTemplate reply message success.");
                }
            } else {
                log.error("replyTemplate reply message failed", e);
            }
        };
        if (result instanceof org.springframework.messaging.Message<?> springMessage) {
            org.springframework.messaging.Message<?> replySpringMessage;
            if (replyHeaderSupplier == null || ObjectUtils.isEmpty(replyHeaderSupplier.get())) {
                replySpringMessage = MessageBuilder.fromMessage(springMessage).build();
            } else {
                replySpringMessage = MessageBuilder.fromMessage(springMessage).copyHeaders(replyHeaderSupplier.get()).build();
            }
            this.replyTemplate.sendAsync(replyTopic, replySpringMessage, sendConsumer);
        } else {
            this.replyTemplate.sendAsync(replyTopic, result, replyHeaderSupplier, null, sendConsumer);
        }
    }

    private String evaluateReplyTopic(Object request, Object source, Object result) {
        String replyTopic;
        if (result instanceof InvocationResult invocationResult) {
            replyTopic = evaluateTopic(request, source, result, invocationResult.sendTo());
        }
        else if (this.replyTopicExpression != null) {
            replyTopic = evaluateTopic(request, source, result, this.replyTopicExpression);
        }
        else {
            replyTopic = getDefaultReplyTopic(request);
        }
        return replyTopic;
    }

    private String evaluateTopic(Object request, Object source, Object result, Expression sendTo) {
        if (sendTo instanceof LiteralExpression) {
            return sendTo.getValue(String.class);
        }
        else {
            Object value = sendTo == null ? null
                    : sendTo.getValue(this.evaluationContext, new ReplyExpressionRoot(request, source, result));

            if (!(value == null || value instanceof String || value instanceof byte[])) {
                throw new IllegalStateException(
                        "replyTopic expression must evaluate to a String or byte[], it is: "
                                + value.getClass().getName());
            }
            if (value instanceof byte[] byteValue) {
                return new String(byteValue, StandardCharsets.UTF_8);
            }
            return (String) value;
        }
    }

    private String getDefaultReplyTopic(Object request) {
        if (request instanceof Message requestMessage) {
            String cluster = requestMessage.getProperty(MessageConst.PROPERTY_CLUSTER);
            if (cluster != null) {
                return MixAll.getReplyTopic(cluster);
            }
        }
        return null;
    }

    protected Type determineInferredType(Method method) { // NOSONAR complexity
        if (method == null) {
            return null;
        }
        Type genericParameterType = null;

        boolean rocketMessageFound = false;
        boolean collectionFound = false;

        for (int i = 0; i < method.getParameterCount(); i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            Type parameterType = methodParameter.getGenericParameterType();

            if (methodParameter.hasParameterAnnotation(Header.class) || methodParameter.hasParameterAnnotation(Headers.class)) {
                this.headerFound = true;
            }
            else if (parameterIsType(parameterType, org.springframework.messaging.Message.class)) {
                this.isSpringMessage = true;
            }
            else if (parameterIsType(parameterType, Message.class)) {
                rocketMessageFound = true;
            }
            else if (isMultipleMessageType(parameterType)) {
                collectionFound = true;
            }
        }

        if (!this.headerFound && !this.isSpringMessage && !rocketMessageFound && !collectionFound) {
            this.simpleExtraction = true;
        }

        for (int i = 0; i < method.getParameterCount(); i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            /*
             * We're looking for a single non-annotated parameter, or one annotated
             * with @Payload. We ignore parameters with type Message, because they
             * are not involved with conversion.
             */
            Type parameterType = methodParameter.getGenericParameterType();
            boolean isNotConvertible = parameterIsType(parameterType, Message.class);

            boolean withPayloadOrWithoutAnnotation = methodParameter.getParameterAnnotations().length == 0
                    || methodParameter.hasParameterAnnotation(Payload.class);
            if (!isNotConvertible && !isMessageWithNoTypeInfo(parameterType) && withPayloadOrWithoutAnnotation) {
                if (genericParameterType == null) {
                    genericParameterType = extractGenericParameterTypFromMethodParameter(methodParameter);
                }
                else {
                    log.debug("Ambiguous parameters for target payload for method {}; no inferred type available", method);
                    break;
                }
            }
        }

        this.messageReturnType = returnTypeMessageOrCollectionOf(method);
        return genericParameterType;
    }

    /**
     * Determines if a type is one that holds multiple messages.
     * @param type the type to check
     * @return true if the type is a {@link List} or a {@link MessageBatch}, false otherwise
     */
    protected boolean isMultipleMessageType(Type type) {
        return parameterIsType(type, List.class) || parameterIsType(type, MessageBatch.class);
    }

    private Type extractGenericParameterTypFromMethodParameter(MethodParameter methodParameter) {
        Type genericParameterType = methodParameter.getGenericParameterType();
        if (genericParameterType instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType().equals(org.springframework.messaging.Message.class)) {
                genericParameterType = ((ParameterizedType) genericParameterType).getActualTypeArguments()[0];
            }
            else if (parameterizedType.getRawType().equals(List.class)
                    && parameterizedType.getActualTypeArguments().length == 1) {

                Type paramType = parameterizedType.getActualTypeArguments()[0];
                this.isRocketMessageList = paramType instanceof ParameterizedType
                        && ((ParameterizedType) paramType).getRawType().equals(Message.class);
                boolean messageHasGeneric = paramType instanceof ParameterizedType
                        && ((ParameterizedType) paramType).getRawType()
                        .equals(org.springframework.messaging.Message.class);
                this.isSpringMessageList = paramType.equals(org.springframework.messaging.Message.class)
                        || messageHasGeneric;
                if (messageHasGeneric) {
                    genericParameterType = ((ParameterizedType) paramType).getActualTypeArguments()[0];
                }
                else if (!this.isSpringMessageList && !this.isRocketMessageList) {
                    genericParameterType = paramType;
                }

                if (!this.isSpringMessageList && !this.isRocketMessageList && !this.headerFound) {
                    this.simpleExtraction = true;
                }
            }
            else {
                this.isRocketMessages = parameterizedType.getRawType().equals(MessageBatch.class);
            }
        }
        return genericParameterType;
    }

    protected boolean parameterIsType(Type parameterType, Type type) {
        if (parameterType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(type)) {
                return true;
            }
        }
        return parameterType.equals(type);
    }

    private boolean isMessageWithNoTypeInfo(Type parameterType) {
        if (parameterType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(org.springframework.messaging.Message.class)) {
                return parameterizedType.getActualTypeArguments()[0] instanceof WildcardType;
            }
        }
        return parameterType.equals(org.springframework.messaging.Message.class);
    }


    /**
     * Return true if the method return type is {@link org.springframework.messaging.Message} or
     * {@code Collection<Message<?>>}.
     * @param method the method.
     * @return true if it returns message(s).
     */
    public boolean returnTypeMessageOrCollectionOf(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType.equals(org.springframework.messaging.Message.class)) {
            return true;
        }
        if (returnType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(org.springframework.messaging.Message.class)) {
                return true;
            }
            if (rawType.equals(Collection.class)) {
                Type collectionType = parameterizedType.getActualTypeArguments()[0];
                if (collectionType.equals(org.springframework.messaging.Message.class)) {
                    return true;
                }
                return collectionType instanceof ParameterizedType
                        && ((ParameterizedType) collectionType).getRawType()
                        .equals(org.springframework.messaging.Message.class);
            }
        }
        return false;

    }

    public record ReplyExpressionRoot(Object request, Object source, Object result) {
    }

}