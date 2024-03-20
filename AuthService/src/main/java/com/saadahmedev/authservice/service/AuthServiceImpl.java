package com.saadahmedev.authservice.service;

import com.saadahmedev.authservice.dto.*;
import com.saadahmedev.authservice.entity.Token;
import com.saadahmedev.authservice.entity.User;
import com.saadahmedev.authservice.repository.TokenRepository;
import com.saadahmedev.authservice.repository.UserRepository;
import com.saadahmedev.authservice.util.JwtUtil;
import com.saadahmedev.authservice.util.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestValidator requestValidator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRepository tokenRepository;

    @Override
    public ResponseEntity<?> createAccount(CreateAccountRequest createAccountRequest) {
        ResponseEntity<?> validationResult = requestValidator.isCreateAccountRequestValid(createAccountRequest);
        if (validationResult.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) return validationResult;

        User user = User.builder()
                .name(createAccountRequest.getName())
                .email(createAccountRequest.getEmail())
                .password(passwordEncoder.encode(createAccountRequest.getPassword()))
                .build();

        try {
            userRepository.save(user);
            return new ResponseEntity<>(new ApiResponse(true, "User created successfully"), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> login(LoginRequest loginRequest) {
        ResponseEntity<?> validationResult = requestValidator.isLoginRequestvalid(loginRequest);
        if (validationResult.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) return validationResult;

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
        String jwtToken = jwtUtil.generateToken(userDetails);

        try {
            tokenRepository.save(new Token(jwtToken));
            return new ResponseEntity<>(new LoginResponse(true, "Logged in successfully", jwtToken), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String requestHeader = request.getHeader("Authorization");
        String token = requestHeader.substring(7);

        try {
            tokenRepository.deleteById(token);
            return new ResponseEntity<>(new ApiResponse(true, "Logged out successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiResponse> validateToken(Token token) {
        if (tokenRepository.findById(token.getToken()).isEmpty()) return sendUnauthorizedResponse();

        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtil.getUsernameFromToken(token.getToken()));
        if (!jwtUtil.validateToken(token.getToken(), userDetails)) return sendUnauthorizedResponse();

        return new ResponseEntity<>(new ApiResponse(true, "Request Authorization Success"), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserResponse> getUser(Token token) {
        String username = jwtUtil.getUsernameFromToken(token.getToken());
        Optional<User> optionalUser = userRepository.findByEmail(username);
        return optionalUser.map(user -> new ResponseEntity<>(new UserResponse(user.getId(), user.getName(), user.getEmail()), HttpStatus.OK)).orElse(null);
    }

    private ResponseEntity<ApiResponse> sendUnauthorizedResponse() {
        return new ResponseEntity<>(new ApiResponse(false, "Token is not valid"), HttpStatus.UNAUTHORIZED);
    }
}
