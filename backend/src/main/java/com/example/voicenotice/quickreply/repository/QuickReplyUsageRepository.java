package com.example.voicenotice.quickreply.repository;

import com.example.voicenotice.quickreply.entity.QuickReplyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuickReplyUsageRepository extends JpaRepository<QuickReplyUsage, Long> {

    @Query("""
            SELECT u.replyCode, u.replyText, COUNT(u)
            FROM QuickReplyUsage u
            GROUP BY u.replyCode, u.replyText
            ORDER BY COUNT(u) DESC
            """)
    List<Object[]> countByReplyCode();
}