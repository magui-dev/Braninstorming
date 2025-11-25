package com.brainstorming.brainstorming_platform.domain.inquiry.repository;

import com.brainstorming.brainstorming_platform.domain.inquiry.entity.Inquiry;
import com.brainstorming.brainstorming_platform.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    //특정 사용자의 문의 목록
    List<Inquiry> findByUserId(Long userId);


    //상태별 문의 목록
    List<Inquiry> findByStatus(InquiryStatus status);

    //특정 사용자의 답변 대기 문의
    List<Inquiry> findByUserIdAndStatus(Long userId, InquiryStatus status);
}
