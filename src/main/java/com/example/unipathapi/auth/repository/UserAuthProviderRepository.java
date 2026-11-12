package com.example.unipathapi.auth.repository;

import com.example.unipathapi.auth.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Integer> {

    Optional<UserAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserAuthProvider> findByUserIdAndProvider(Integer userId, String provider);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserIdAndProvider(Integer userId, String provider);
}

