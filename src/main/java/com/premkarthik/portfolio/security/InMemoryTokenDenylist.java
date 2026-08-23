package com.premkarthik.portfolio.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback used by the default (H2) profile, where MongoDB is not wired up.
 * Revocations are lost on restart, which is acceptable because every token
 * outlives the process only if the process is still running.
 */
@Component
@ConditionalOnProperty(name = "auth.token-denylist.persistent", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenDenylist implements TokenDenylist {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String token, Instant expiresAt) {
        purgeExpired();
        revoked.put(TokenDenylist.fingerprint(token), expiresAt);
    }

    @Override
    public boolean isRevoked(String token) {
        Instant expiresAt = revoked.get(TokenDenylist.fingerprint(token));
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revoked.remove(TokenDenylist.fingerprint(token));
            return false;
        }
        return true;
    }

    /** Keeps the map bounded by the number of tokens that are still live. */
    private void purgeExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
