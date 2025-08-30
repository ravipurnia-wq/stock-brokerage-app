package com.stockbrokerage.controller;

import com.stockbrokerage.dto.LoginRequest;
import com.stockbrokerage.dto.UserRegistrationRequest;
import com.stockbrokerage.entity.User;
import com.stockbrokerage.security.JwtTokenProvider;
import com.stockbrokerage.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            User user = userService.findByEmail(request.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", jwt);
            response.put("tokenType", "Bearer");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "status", user.getStatus(),
                    "kycStatus", user.getKycStatus()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error authenticating user: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authentication failed", "message", "Invalid credentials", "details", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email);

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "status", user.getStatus(),
                    "kycStatus", user.getKycStatus(),
                    "roles", user.getRoles()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get user info", "message", e.getMessage()));
        }
    }
}