package org.springframework.rocket.test.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author lanpengfei
 * @date 2023/12/18
 */
@Getter
@RequiredArgsConstructor
public abstract class Response implements Serializable {

    private final int code;
    private final String message;

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MESSAGE = "SUCCESS";

    public Response() {
        this(SUCCESS_CODE, SUCCESS_MESSAGE);
    }

    public boolean isSuccess() {
        return Objects.equals(SUCCESS_CODE, this.code);
    }
}
