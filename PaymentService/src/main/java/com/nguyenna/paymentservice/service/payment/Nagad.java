package com.nguyenna.paymentservice.service.payment;

import com.nguyenna.paymentservice.dto.OrderEvent;
import com.nguyenna.paymentservice.dto.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Nagad implements PaymentMethod {

    @Autowired
    private PaymentService paymentService;

    @Override
    public void makePayment(OrderEvent orderEvent) {
        paymentService.makePayment(orderEvent, PaymentStatus.PENDING, "Payment is in pending state");
    }
}
