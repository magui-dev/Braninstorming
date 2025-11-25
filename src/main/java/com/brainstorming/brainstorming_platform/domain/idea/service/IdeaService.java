package com.brainstorming.brainstorming_platform.domain.idea.service;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import com.brainstorming.brainstorming_platform.domain.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaRepository ideaRepository;

    /**
     * 아이디어 저장
     */
    public Idea save(Idea idea) {
        return ideaRepository.save(idea);
    }

    /**
     * ID로 아이디어 조회
     */
    public Idea findById(Long ideaId) {
        return ideaRepository.findById(ideaId)
                .orElseThrow(() -> new RuntimeException("저장된 아이디어가 없습니다."));
    }


    /**
     * 저장된 userID의 모든 아이디어를 조회
     */
    public List<Idea> findByUserId(Long userId) {
        return ideaRepository.findByUserId(userId);
    }

    /**
     * 아이디어 삭제
     */
    public void delete(Long ideaId) {
        ideaRepository.deleteById(ideaId);
    }

    /**
     * ID의 아이디어 갯수
     */
    public long countByUserId(Long userId) {
        return ideaRepository.countByUserId(userId);
    }

}
