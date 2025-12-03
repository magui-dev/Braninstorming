package com.brainstorming.brainstorming_platform.domain.inquiry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryUpdateRequestDto {
    private String title;
    private String content;

}
