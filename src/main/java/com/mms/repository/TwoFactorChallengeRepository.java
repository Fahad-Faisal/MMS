// src/main/java/com/mms/repository/TwoFactorChallengeRepository.java
package com.mms.repository;

import com.mms.entity.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, String> {
    Optional<TwoFactorChallenge> findByIdAndUserId(String id, String userId);
    void deleteByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM TwoFactorChallenge c WHERE c.userId = :userId AND c.purpose = 'PASSWORD_RESET'")
    void deleteAllPasswordResetChallengesByUserId(@Param("userId") String userId);
}