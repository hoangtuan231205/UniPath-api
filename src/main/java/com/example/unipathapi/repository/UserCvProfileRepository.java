package com.example.unipathapi.repository;

import com.example.unipathapi.entity.UserCvProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCvProfileRepository extends JpaRepository<UserCvProfile, Integer> {

    List<UserCvProfile> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Modifying
    @Query("UPDATE UserCvProfile p SET p.isPrimary = false WHERE p.user.id = :userId")
    void resetPrimaryForUser(@Param("userId") Integer userId);
}
