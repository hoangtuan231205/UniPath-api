package com.example.unipathapi.repository;

import com.example.unipathapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    long countByRole(String role);

    long countByRoleAndIsActiveTrue(String role);

    List<User> findByRole(String role);
}
