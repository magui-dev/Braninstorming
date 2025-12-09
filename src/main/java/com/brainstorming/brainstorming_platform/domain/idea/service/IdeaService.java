package com.brainstorming.brainstorming_platform.domain.idea.service;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import com.brainstorming.brainstorming_platform.domain.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 게스트 세션의 아이디어를 로그인한 사용자에게 연결
     */
    @Transactional
    public int linkGuestIdeasToUser(String guestSessionId, Long userId) {
        List<Idea> guestIdeas = ideaRepository.findByGuestSessionId(guestSessionId);

        for (Idea idea : guestIdeas) {
            //기존 아이디어에 userId설정하고 guestSessionId제거
            Idea linkedIdea = new Idea(
                    idea.getIdeaId(),
                    userId,
                    idea.getTitle(),
                    idea.getContent(),
                    idea.getPurpose(),
                    null // guestSessionId 제거
            );
            ideaRepository.save(linkedIdea);
        }
        return guestIdeas.size(); //연결된 아이디어 개수 반환
    }

}
