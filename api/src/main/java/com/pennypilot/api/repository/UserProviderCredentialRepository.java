package com.pennypilot.api.repository;

import com.pennypilot.api.entity.UserProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProviderCredentialRepository extends JpaRepository<UserProviderCredential, Long> {

    Optional<UserProviderCredential> findByUserIdAndProviderId(Long userId, Long providerId);

    void deleteByUserId(Long userId);
}
