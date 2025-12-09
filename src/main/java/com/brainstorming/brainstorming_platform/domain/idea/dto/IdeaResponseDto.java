package com.brainstorming.brainstorming_platform.domain.idea.dto;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IdeaResponseDto {
    private Long ideaId;
    private Long userId;
    private String title;
    private String content;
    private String purpose;
    private LocalDateTime createdAt;  // 추가!

    public static IdeaResponseDto from (Idea idea) {
        return new IdeaResponseDto(
                idea.getIdeaId(),
                idea.getUserId(),
                idea.getTitle(),
                idea.getContent(),
                idea.getPurpose(),
                idea.getCreatedAt()  // 추가!
        );
    }
}
