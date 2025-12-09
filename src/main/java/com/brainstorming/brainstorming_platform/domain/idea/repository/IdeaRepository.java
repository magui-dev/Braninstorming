package com.brainstorming.brainstorming_platform.domain.idea.repository;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long>  {

    //특정 사용자의 모든 아이디어 조회
    List<Idea> findByUserId(Long userId);

    //특정 사용자의 아이디어 갯수
    long countByUserId(Long userId);

    // 게스느 세션 ID로 아이디어 조회 (로그인후 연결용)
    List<Idea> findByGuestSessionId(String guestSessionId);
    /**
     * 오래된 게스트 아이디어 삭제(고아 DB 삭제)
     * -userId가 NULL
     * -guestSessionId 존재
     * -생성일이 기준일 이전인 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM Idea i WHERE i.userId IS NULL AND i.guestSessionId IS NOT NULL AND i.createdAt < :cutoffDate")
    int deleteOldGuestIdeas(@Param("cutoffDate") LocalDateTime cutoffDate);

}
