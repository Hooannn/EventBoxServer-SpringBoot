package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.FCMToken;
import com.ht.eventbox.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    Optional<FCMToken> findByUserId(Long userId);
}
