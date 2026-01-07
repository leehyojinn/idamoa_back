package com.hip.damoa.domain.community.repository;

import com.hip.damoa.domain.community.model.CommunityPostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostAttachmentRepository extends JpaRepository<CommunityPostAttachment, Long> {

    List<CommunityPostAttachment> findByPostIdOrderByDisplayOrderAsc(Long postId);

    @Query("SELECT a FROM CommunityPostAttachment a WHERE a.post.id IN :postIds ORDER BY a.displayOrder ASC")
    List<CommunityPostAttachment> findByPostIdIn(@Param("postIds") List<Long> postIds);

    void deleteByPostId(Long postId);
}
