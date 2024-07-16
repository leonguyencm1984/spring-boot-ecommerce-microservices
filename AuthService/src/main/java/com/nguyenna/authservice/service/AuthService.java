package com.nguyenna.authservice.service;

import com.nguyenna.authservice.dto.ApiResponse;
import com.nguyenna.authservice.dto.CreateAccountRequest;
import com.nguyenna.authservice.dto.LoginRequest;
import com.nguyenna.authservice.entity.Token;
import com.nguyenna.authservice.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    ResponseEntity<?> register(CreateAccountRequest createAccountRequest);

    ResponseEntity<?> login(LoginRequest loginRequest);

    ResponseEntity<?> logout(HttpServletRequest request);

    ResponseEntity<ApiResponse> validateToken(Token token);

    ResponseEntity<UserResponse> getUser(Token token);
}
