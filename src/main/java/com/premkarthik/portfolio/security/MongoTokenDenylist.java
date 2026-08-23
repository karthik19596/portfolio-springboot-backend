package com.premkarthik.portfolio.security;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.premkarthik.portfolio.model.RevokedToken;
import com.premkarthik.portfolio.repository.RevokedTokenRepository;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "auth.token-denylist.persistent", havingValue = "true")
public class MongoTokenDenylist implements TokenDenylist {

    private static final Logger log = LoggerFactory.getLogger(MongoTokenDenylist.class);
    private static final String COLLECTION = "revoked_tokens";
    private static final String TTL_INDEX = "expiresAt_ttl";

    private final RevokedTokenRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoTokenDenylist(RevokedTokenRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Builds the TTL index by hand. Spring Data's @Indexed(expireAfter = "0s")
     * silently drops the zero-length duration and creates an ordinary index,
     * which would let this collection grow without bound.
     */
    @PostConstruct
    void ensureTtlIndex() {
        MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);

        // Mongo rejects a second index on the same key under a different name,
        // so any non-TTL leftover has to go first.
        for (Document index : collection.listIndexes()) {
            String name = index.getString("name");
            if (index.get("key", Document.class).containsKey("expiresAt")
                    && !TTL_INDEX.equals(name)) {
                log.info("Replacing non-TTL index '{}' on {}", name, COLLECTION);
                collection.dropIndex(name);
            }
        }

        collection.createIndex(
                new Document("expiresAt", 1),
                new IndexOptions().name(TTL_INDEX).expireAfter(0L, TimeUnit.SECONDS)
        );
    }

    @Override
    public void revoke(String token, Instant expiresAt) {
        repository.save(new RevokedToken(
                TokenDenylist.fingerprint(token),
                null,
                expiresAt,
                Instant.now()
        ));
    }

    @Override
    public boolean isRevoked(String token) {
        return repository.existsById(TokenDenylist.fingerprint(token));
    }
}
