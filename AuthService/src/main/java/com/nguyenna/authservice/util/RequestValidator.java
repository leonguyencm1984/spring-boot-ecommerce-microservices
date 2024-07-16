package com.nguyenna.authservice.util;

import com.nguyenna.authservice.repository.UserRepository;
import com.nguyenna.authservice.dto.ApiResponse;
import com.nguyenna.authservice.dto.CreateAccountRequest;
import com.nguyenna.authservice.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

    private final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<?> isCreateAccountRequestValid(CreateAccountRequest createAccountRequest) {
        if (createAccountRequest.getLastName() == null || createAccountRequest.getLastName().isEmpty()) return generateBadRequest("Last Name is required");
        if(createAccountRequest.getFirstName() == null || createAccountRequest.getFirstName().isEmpty()) return generateBadRequest("First Name is required");
        if (createAccountRequest.getEmail() == null || createAccountRequest.getEmail().isEmpty()) return generateBadRequest("Email is required");
        if (!createAccountRequest.getEmail().matches(EMAIL_PATTERN)) return generateBadRequest("Invalid email address");
        if (userRepository.findByEmail(createAccountRequest.getEmail()).isPresent()) return generateBadRequest("Email already exist");
        if (createAccountRequest.getPassword() == null || createAccountRequest.getPassword().isEmpty()) return generateBadRequest("Password is required");
        if (createAccountRequest.getConfirmPassword() == null || createAccountRequest.getConfirmPassword().isEmpty()) return generateBadRequest("Confirm password is required");
        if (!createAccountRequest.getPassword().equals(createAccountRequest.getConfirmPassword())) return generateBadRequest("Password did not match");

        return ResponseEntity.ok().body(null);
    }

    public ResponseEntity<?> isLoginRequestvalid(LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) return generateBadRequest("Email is required");
        if (!loginRequest.getEmail().matches(EMAIL_PATTERN)) return generateBadRequest("Invalid email address");
        if (userRepository.findByEmail(loginRequest.getEmail()).isEmpty()) return generateBadRequest("Email does not exist");
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) return generateBadRequest("Password is required");

        return ResponseEntity.ok().body(null);
    }

    private ResponseEntity<?> generateBadRequest(String message) {
        return new ResponseEntity<>(new ApiResponse(false, message), HttpStatus.BAD_REQUEST);
    }
}
