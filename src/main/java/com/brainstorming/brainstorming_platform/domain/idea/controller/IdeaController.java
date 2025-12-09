package com.brainstorming.brainstorming_platform.domain.idea.controller;

import com.brainstorming.brainstorming_platform.domain.idea.dto.IdeaRequestDto;
import com.brainstorming.brainstorming_platform.domain.idea.dto.IdeaResponseDto;
import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import com.brainstorming.brainstorming_platform.domain.idea.service.IdeaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    //Idea 생성
    @PostMapping
    public ResponseEntity<IdeaResponseDto> createIdea(@RequestBody IdeaRequestDto requestDto) {
        // 1 Dto -> Entity 변환
        Idea idea = requestDto.toEntity();
        // 2 Service 호출
        Idea saveIdea = ideaService.save(idea);
        // 3 Entity 를 ResponseDto 전환
        IdeaResponseDto responseDto = IdeaResponseDto.from(saveIdea);
        // 4 응답
        return ResponseEntity.ok(responseDto);
    }

    //Idea 조회
    @GetMapping("/{id}")
    public ResponseEntity<IdeaResponseDto> getIdea(@PathVariable("id") Long ideaId) {
        // 1 Service 호출
        Idea findIdea = ideaService.findById(ideaId);
        // 2 Dto 변환
        IdeaResponseDto responseDto = IdeaResponseDto.from(findIdea);
        // 3 응답
        return ResponseEntity.ok(responseDto);
    }

    //Idea 전체조회
    @GetMapping
    public ResponseEntity<List<IdeaResponseDto>> getIdeasByUser(@RequestParam Long userId) {
        // 1 Service 호출
        List<Idea> ideas = ideaService.findByUserId(userId);
        // 2 Dto 변환
        List<IdeaResponseDto> responseDtos = ideas.stream()
                .map(IdeaResponseDto::from)
                .toList();
        // 3 응답
        return ResponseEntity.ok(responseDtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIdea(@PathVariable("id") Long ideaId) {
        // 1 Service 호출
        ideaService.delete(ideaId);
        // 2 응답
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countIdeas(@RequestParam Long userId) {
        // 1 서비스 호출
        long countIdeas = ideaService.countByUserId(userId);
        // 저장된 카운트 응답
        return ResponseEntity.ok(countIdeas);
    }

    /**
     * 로그인 후 게스트아이디어를 사용자에게 연결
     * POST /api/ideas/link-guest
     */
    @PostMapping("/link-guest")
    public ResponseEntity<Integer> linkGuestIdeas(
            @RequestParam String guestSessionId,
            @RequestParam Long userId) {
        int linkedCount = ideaService.linkGuestIdeasToUser(guestSessionId, userId);
        return ResponseEntity.ok(linkedCount);
    }
}
