package com.mms.repository;

import com.mms.entity.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, String> {
    Optional<TwoFactorChallenge> findByIdAndUserId(String id, String userId);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
