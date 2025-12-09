package com.brainstorming.brainstorming_platform.domain.idea.scheduler;

import com.brainstorming.brainstorming_platform.domain.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdeaCleanupScheduler {

    private final IdeaRepository ideaRepository;

    /**
     * 오래된 게스트 아이디어 정리 (매일 새벽 3시 실행)
     * - 전일(1일) 이전에 생성한 게스트 아이디어 삭제
     */
    @Scheduled(cron = "0 0 3 * * *") //매일 새벽 3시
    @Transactional
    public void cleanupOldGuestIdeas() {
        log.info("게스트 아이디어 정리 시작 ... ");

        // 1일 전 기준
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        int deletedCount = ideaRepository.deleteOldGuestIdeas(cutoffDate);

        log.info("게스트 아이디어 정리 완료: {}개 삭제 (기준: {} 이전)", deletedCount, cutoffDate);
    }
}
