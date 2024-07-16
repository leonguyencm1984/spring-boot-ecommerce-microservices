package com.nguyenna.cartservice.feing;

import com.nguyenna.cartservice.dto.ApiResponse;
import com.nguyenna.cartservice.dto.Token;
import com.nguyenna.cartservice.dto.UserResponse;
import com.nguyenna.cartservice.util.AppConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@FeignClient(name = "AUTH-SERVICE", url = AppConstants.BASE_URL)
public interface AuthService {

    @PostMapping("api/auth/validate-token")
    ResponseEntity<ApiResponse> validateToken(@RequestBody Token token);

    @GetMapping("api/auth/user")
    ResponseEntity<UserResponse> getUser(@RequestBody Token token);
}
