package com.nguyenna.paymentservice.service.payment;

import com.nguyenna.paymentservice.dto.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public interface PaymentMethod {
    void makePayment(OrderEvent orderEvent);
}
