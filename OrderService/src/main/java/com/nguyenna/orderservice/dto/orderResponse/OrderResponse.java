package com.nguyenna.orderservice.dto.orderResponse;

import com.nguyenna.orderservice.entity.DeliveryStatus;
import com.nguyenna.orderservice.entity.PaymentStatus;
import com.nguyenna.orderservice.entity.ShippingDetails;
import com.nguyenna.orderservice.dto.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private long id;
    private double totalPrice;
    private int discount;
    private double grandTotal;
    private List<Product> products;
    private DeliveryStatus deliveryStatus;
    private ShippingDetails shippingDetails;
    private PaymentStatus paymentStatus;
    private Date createdAt;
    private Date updatedAt;
}
