package com.example.unipathapi.repository;

import com.example.unipathapi.entity.PostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Integer> {

    @Query("SELECT c FROM PostComment c WHERE c.post.id = :postId " +
           "AND (:cursor IS NULL OR c.id < :cursor) " +
           "ORDER BY c.id DESC")
    List<PostComment> findCommentsByPostId(@Param("postId") Integer postId,
                                           @Param("cursor") Integer cursor,
                                           Pageable pageable);

    long countByPostId(Integer postId);
}
