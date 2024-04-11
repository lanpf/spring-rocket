package org.springframework.rocket.test.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

/**
 * @author lanpengfei
 * @date 2023/12/18
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class Result<T> extends Response {

    private T data;

    public Result() {
        super();
    }

    public Result(T data) {
        super();
        this.data = data;
    }

    public Result(int code, String message) {
        this(code, message, null);
    }

    public Result(int code, String message, T data) {
        super(code, message);
        this.data = data;
        if (!isSuccess() && Objects.nonNull(data)) {
            throw new IllegalArgumentException("data must be null if response code is not success");
        }
    }

    public static <T> Result<T> of(T data) {
        return new Result<>(data);
    }

    public static Result<Void> success() {
        return new Result<>();
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message);
    }
}
