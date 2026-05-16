package com.example.voicenotice.notice.repository;

import com.example.voicenotice.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Optional<Notice> findByFinalTranscript_Id(Long finalTranscriptId);
}