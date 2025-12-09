package com.brainstorming.brainstorming_platform.domain.idea.dto;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class IdeaRequestDto {
    private Long userId;
    private String title;
    private String content;
    private String purpose;
    private String guestSessionId;

    public Idea toEntity() {
        return new Idea(
                null,  //IdeaId 자동생성
                userId,
                title,
                content,
                purpose,       //아이디어 요청내용
                guestSessionId
        );
    }
}
