package com.premkarthik.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "revoked_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    /** SHA-256 digest of the token, never the token itself. */
    @Id
    private String tokenHash;

    private String username;

    /**
     * Mongo drops the document once this instant passes, so the collection
     * cannot outgrow the number of tokens that are still live. The TTL index
     * is built in MongoTokenDenylist: @Indexed(expireAfter = "0s") is ignored
     * by Spring Data, which skips zero-length durations and leaves a plain
     * index behind.
     */
    private Instant expiresAt;

    private Instant revokedAt;
}
