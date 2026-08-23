package com.premkarthik.portfolio.controller;

import com.premkarthik.portfolio.dto.ApiResponse;
import com.premkarthik.portfolio.dto.AuthResponse;
import com.premkarthik.portfolio.dto.LoginRequest;
import com.premkarthik.portfolio.dto.SignupRequest;
import com.premkarthik.portfolio.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Sign up and login endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(ApiResponse.success(authService.isUsernameAvailable(username)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(authService.isEmailAvailable(email)));
    }
}
