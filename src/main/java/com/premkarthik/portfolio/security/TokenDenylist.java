package com.premkarthik.portfolio.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Revocation list for JWTs that have been logged out before their `exp`.
 * A stateless token is otherwise accepted until it expires, so signing out
 * would leave any copy of it usable for the rest of its lifetime.
 */
public interface TokenDenylist {

    void revoke(String token, Instant expiresAt);

    boolean isRevoked(String token);

    /**
     * Entries are keyed by digest so a database dump never yields usable
     * bearer tokens.
     */
    static String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
