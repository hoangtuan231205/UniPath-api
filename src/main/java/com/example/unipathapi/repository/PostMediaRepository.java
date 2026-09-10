package com.example.unipathapi.repository;

import com.example.unipathapi.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Integer> {
    List<PostMedia> findByPostIdOrderByDisplayOrderAsc(Integer postId);
    long countByPostId(Integer postId);
}
