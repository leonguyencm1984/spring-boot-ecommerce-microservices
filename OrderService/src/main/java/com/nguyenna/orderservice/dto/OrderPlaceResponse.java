package com.nguyenna.orderservice.dto;

public record OrderPlaceResponse(boolean status, String message, long orderId) {
}
