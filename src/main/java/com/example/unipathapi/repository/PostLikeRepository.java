package com.example.unipathapi.repository;

import com.example.unipathapi.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);
    long countByPostId(Integer postId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    Set<Integer> findLikedPostIdsByUserIdAndPostIds(@Param("userId") Integer userId, @Param("postIds") Collection<Integer> postIds);
}
