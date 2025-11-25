package com.brainstorming.brainstorming_platform.domain.inquiry.service;

import com.brainstorming.brainstorming_platform.domain.inquiry.entity.Inquiry;
import com.brainstorming.brainstorming_platform.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 문의사항 저장
     */
    public Inquiry save(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }

    /**
     * ID 별 문의사항 찾기
     */
    public Inquiry findById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(()-> new RuntimeException("저장된 문의사항이 없습니다."));
    }

    /**
     * Id로 유저 모든 문의 찾기
     */
    public List<Inquiry> findByUserId(Long userId) {
        return inquiryRepository.findByUserId(userId);
    }

    /**
     * 문의사항 삭제
     */
    public void delete(Long inquiryId) {
        inquiryRepository.deleteById(inquiryId);
    }

}
