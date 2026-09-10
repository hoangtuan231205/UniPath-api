package com.example.unipathapi.community.repository;
import com.example.unipathapi.community.entity.*;

import com.example.unipathapi.community.entity.CommunityPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {

    @Query("SELECT p FROM CommunityPost p WHERE p.isActive = true " +
           "AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.createdAt DESC, p.id DESC")
    List<CommunityPost> findPostsFeed(@Param("cursor") Integer cursor, Pageable pageable);

    @Query("SELECT p FROM CommunityPost p WHERE p.author.id = :userId AND p.isActive = true " +
           "AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.createdAt DESC, p.id DESC")
    List<CommunityPost> findUserPostsFeed(@Param("userId") Integer userId, @Param("cursor") Integer cursor, Pageable pageable);
}
