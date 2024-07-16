package com.nguyenna.authservice.controller;

import com.nguyenna.authservice.dto.ApiResponse;
import com.nguyenna.authservice.dto.CreateAccountRequest;
import com.nguyenna.authservice.dto.LoginRequest;
import com.nguyenna.authservice.entity.Token;
import com.nguyenna.authservice.service.AuthService;
import com.nguyenna.authservice.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${application.mode}")
    private String applicationMode;

    @PostMapping("/register")
    @CircuitBreaker(name = "register", fallbackMethod = "fallback")
    public ResponseEntity<?> register(@RequestBody CreateAccountRequest createAccountRequest) {
        return authService.register(createAccountRequest);
    }

    @PostMapping("/login")
    @CircuitBreaker(name = "login", fallbackMethod = "fallback")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @GetMapping("/logout")
    @CircuitBreaker(name = "logout", fallbackMethod = "fallback")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/validate-token")
    @CircuitBreaker(name = "validateToken", fallbackMethod = "fallback")
    public ResponseEntity<?> validateToken(@RequestBody Token token) {
        return authService.validateToken(token);
    }

    @PostMapping("/user")
    @CircuitBreaker(name = "getUser", fallbackMethod = "fallback")
    public ResponseEntity<UserResponse> getUser(@RequestBody Token token) {
        return authService.getUser(token);
    }

    private ResponseEntity<?> fallback(Exception e) {
        return new ResponseEntity<>(new ApiResponse(
                false,
                applicationMode.equals("debug") ? e.getLocalizedMessage() : "Some of the services are not working, try in a while."
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
