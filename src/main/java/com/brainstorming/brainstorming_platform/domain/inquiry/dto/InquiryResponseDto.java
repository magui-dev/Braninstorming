package com.brainstorming.brainstorming_platform.domain.inquiry.dto;

import com.brainstorming.brainstorming_platform.domain.inquiry.entity.Inquiry;
import com.brainstorming.brainstorming_platform.domain.inquiry.entity.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InquiryResponseDto {

    private Long inquiryId;
    private Long userId;
    private String title;
    private String content;
    private InquiryStatus status;
    private String reply;
    private LocalDateTime createdAt;  // 추가!

    public static InquiryResponseDto from (Inquiry inquiry) {
        return new InquiryResponseDto(
                inquiry.getInquiryId(),
                inquiry.getUserId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getReply(),
                inquiry.getCreatedAt()  // 추가!
        );
    }
}
