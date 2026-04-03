package com.pennypilot.api.repository;

import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByName(ProviderType name);
}
