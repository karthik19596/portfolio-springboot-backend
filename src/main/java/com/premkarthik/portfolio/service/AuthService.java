package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.dto.AuthResponse;
import com.premkarthik.portfolio.dto.LoginRequest;
import com.premkarthik.portfolio.dto.SignupRequest;
import com.premkarthik.portfolio.dto.UserProfileResponse;
import com.premkarthik.portfolio.exception.ResourceNotFoundException;
import com.premkarthik.portfolio.model.User;
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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenDenylist tokenDenylist;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       TokenDenylist tokenDenylist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.tokenDenylist = tokenDenylist;
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
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole().toUpperCase() : "USER");

        userRepository.save(user);

        return login(new LoginRequest(request.getUsername(), request.getPassword()));
    }

    /**
     * Revokes the presented token for whatever remains of its lifetime.
     * The filter chain has already validated it, so the only thing that can
     * fail here is a caller sending a different, malformed header.
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);
        try {
            tokenDenylist.revoke(token, jwtUtil.getExpiration(token));
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

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtil.generateToken(authentication);

        return new AuthResponse(
                token,
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                "Login successful"
        );
    }
}
