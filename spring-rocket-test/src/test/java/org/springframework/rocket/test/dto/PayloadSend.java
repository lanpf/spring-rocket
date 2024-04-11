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
        return create(UUID.randomUUID().toString());
    }

    public static PayloadSend create(String id) {
        return PayloadSend.builder()
                .id(id)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
