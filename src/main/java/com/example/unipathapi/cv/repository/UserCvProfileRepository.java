package com.example.unipathapi.cv.repository;

import com.example.unipathapi.cv.entity.UserCvProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCvProfileRepository extends JpaRepository<UserCvProfile, Integer> {

    List<UserCvProfile> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<UserCvProfile> findByIdAndUserId(Integer id, Integer userId);

    Optional<UserCvProfile> findByUserIdAndIsPrimaryTrue(Integer userId);

    long countByUserId(Integer userId);

    Optional<UserCvProfile> findFirstByUserIdOrderByUpdatedAtDesc(Integer userId);

    @Modifying
    @Query("UPDATE UserCvProfile p SET p.isPrimary = false WHERE p.user.id = :userId")
    void resetPrimaryForUser(@Param("userId") Integer userId);
}
