package org.springframework.rocket.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayloadSend implements Serializable {
    private String id;
    private Long timestamp;

    public static PayloadSend create() {
        return PayloadSend.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
