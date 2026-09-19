package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.dto.AuthResponse;
import com.premkarthik.portfolio.dto.LoginRequest;
import com.premkarthik.portfolio.dto.PasswordResetConfirmRequest;
import com.premkarthik.portfolio.dto.PasswordResetRequest;
import com.premkarthik.portfolio.dto.RefreshTokenRequest;
import com.premkarthik.portfolio.dto.SignupRequest;
import com.premkarthik.portfolio.dto.UserProfileResponse;
import com.premkarthik.portfolio.exception.AccountStatusException;
import com.premkarthik.portfolio.exception.DuplicateResourceException;
import com.premkarthik.portfolio.exception.InvalidTokenException;
import com.premkarthik.portfolio.exception.ResourceNotFoundException;
import com.premkarthik.portfolio.model.PasswordResetToken;
import com.premkarthik.portfolio.model.RefreshToken;
import com.premkarthik.portfolio.model.User;
import com.premkarthik.portfolio.repository.PasswordResetTokenRepository;
import com.premkarthik.portfolio.repository.RefreshTokenRepository;
import com.premkarthik.portfolio.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import com.premkarthik.portfolio.security.JwtUtil;
import com.premkarthik.portfolio.security.TokenDenylist;
import com.premkarthik.portfolio.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(30);
    private static final Duration PASSWORD_RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenDenylist tokenDenylist;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       TokenDenylist tokenDenylist,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.tokenDenylist = tokenDenylist;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Public registration must never grant elevated roles. Promote users
        // through the protected admin workflow after account creation.
        user.setRole("USER");

        userRepository.save(user);

        return login(new LoginRequest(request.getUsername(), request.getPassword()));
    }

    /**
     * Revokes the presented token for whatever remains of its lifetime.
     * The filter chain has already validated it, so the only thing that can
     * fail here is a caller sending a different, malformed header.
     */
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);
        try {
            tokenDenylist.revoke(token, jwtUtil.getExpiration(token));
            String username = jwtUtil.getUsernameFromToken(token);
            userRepository.findByUsername(username)
                    .ifPresent(user -> refreshTokenRepository.deleteByUser_Id(user.getId()));
        } catch (JwtException | IllegalArgumentException e) {
            // An unreadable token is already unusable; nothing to revoke.
        }
    }

    public UserProfileResponse getProfile(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String status = user.getStatus();
        if (status == null) {
            status = "ACTIVE";
        }
        if (!"ACTIVE".equals(status)) {
            throw new AccountStatusException("Account is not active. Current status: " + status);
        }

        String token = jwtUtil.generateToken(authentication);
        String refreshToken = createRefreshToken(userDetails.getId());

        return new AuthResponse(
                token,
                refreshToken,
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                "Login successful"
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        UserDetailsImpl userDetails = UserDetailsImpl.build(stored.getUser());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(userDetails.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                "Token refreshed"
        );
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser_Id(user.getId());

            String rawToken = generateOpaqueToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setTokenHash(hashToken(rawToken));
            resetToken.setUser(user);
            resetToken.setCreatedAt(Instant.now());
            resetToken.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_LIFETIME));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            // Replace this with an email provider in production. Keeping the
            // raw token out of the API response avoids account takeover if the
            // endpoint is called from an untrusted client.
            System.getLogger(AuthService.class.getName()).log(
                    System.Logger.Level.INFO,
                    "Password reset requested for username {0}. Configure email delivery to send the reset link.",
                    user.getUsername());
        });
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.deleteByUser_Id(user.getId());
    }

    private String createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepository.deleteByUser_Id(userId);

        String rawToken = generateOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_LIFETIME));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
