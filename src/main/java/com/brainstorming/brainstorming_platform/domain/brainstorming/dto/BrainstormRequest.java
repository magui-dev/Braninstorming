package com.brainstorming.brainstorming_platform.domain.brainstorming.dto;

import lombok.Data;
import java.util.List;

/**
 * 사용자 → Java Controller
 * 브레인스토밍 요청
 */
@Data
public class BrainstormRequest {
    private Long userId;                  // 사용자 ID
    private String guestSessionId;        // 비로그인 사용자 임시저장용(추가)
    private String purpose;               // Q1: 목적
    private List<String> associations;    // Q3: 자유연상 키워드
}
