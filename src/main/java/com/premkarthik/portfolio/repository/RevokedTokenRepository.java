package com.premkarthik.portfolio.repository;

import com.premkarthik.portfolio.model.RevokedToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedTokenRepository extends MongoRepository<RevokedToken, String> {
}
