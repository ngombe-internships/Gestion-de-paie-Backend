package com.hades.paie1.repository;

import com.hades.paie1.model.PasswordResetToken;
import com.hades.paie1.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long >{

    Optional<PasswordResetToken> findByToken (String token);

    Optional<PasswordResetToken> findByUser (User user);


    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime dateTime);
}
