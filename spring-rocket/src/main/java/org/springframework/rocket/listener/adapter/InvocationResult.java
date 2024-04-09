package org.springframework.rocket.listener.adapter;

import org.springframework.expression.Expression;

public record InvocationResult(Object result, Expression sendTo, boolean messageReturnType) {
}
