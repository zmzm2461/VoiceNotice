package com.example.voicenotice.quickreply.repository;

import com.example.voicenotice.quickreply.entity.QuickReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuickReplyRepository extends JpaRepository<QuickReply, Long> {

    List<QuickReply> findByActiveTrueOrderByReplyCodeAsc();

    Optional<QuickReply> findByReplyCodeAndActiveTrue(Integer replyCode);
}