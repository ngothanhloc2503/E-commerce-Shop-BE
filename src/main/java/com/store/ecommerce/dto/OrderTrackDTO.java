package com.store.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.store.ecommerce.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderTrackDTO {
    Long id;
    String notes;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    Date updatedTime;
    OrderStatus status;
    Long orderId;
}
