package com.nguyenna.orderservice.dto;

import com.nguyenna.orderservice.dto.kafka.PayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingDetailsRequest {
    private String name;
    private String phone;
    private String email;
    private String address;
    private PayType payType;
}
