package com.nguyenna.apigateway.dto;

public record ApiResponse(int statusCode, boolean status, String message) {
}
