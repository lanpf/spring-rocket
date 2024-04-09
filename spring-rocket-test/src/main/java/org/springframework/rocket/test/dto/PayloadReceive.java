package org.springframework.rocket.test.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PayloadReceive implements Serializable {
    private String id;
    private Long timestamp;
}
