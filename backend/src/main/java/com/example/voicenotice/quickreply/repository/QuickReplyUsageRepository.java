package com.example.voicenotice.quickreply.repository;

import com.example.voicenotice.quickreply.entity.QuickReplyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuickReplyUsageRepository extends JpaRepository<QuickReplyUsage, Long> {

    @Query("""
            SELECT u.replyCode, u.replyText, COUNT(u)
            FROM QuickReplyUsage u
            GROUP BY u.replyCode, u.replyText
            ORDER BY COUNT(u) DESC
            """)
    List<Object[]> countByReplyCode();

    @Query("""
        SELECT u.replyCode, u.replyText, COUNT(u)
        FROM QuickReplyUsage u
        WHERE u.usedAt >= :start
          AND u.usedAt < :end
        GROUP BY u.replyCode, u.replyText
        ORDER BY COUNT(u) DESC
        """)
    List<Object[]> countByReplyCodeAndUsedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}