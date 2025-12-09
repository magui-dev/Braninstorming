package com.brainstorming.brainstorming_platform.domain.inquiry.service;

import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryReplyRequestDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryUpdateRequestDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.entity.Inquiry;
import com.brainstorming.brainstorming_platform.domain.inquiry.entity.InquiryStatus;
import com.brainstorming.brainstorming_platform.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 문의사항 수정(사용자)
     * -PENDING 상태일때 수정가능
     */
    @Transactional
    public Inquiry update(Long inquiryId, InquiryUpdateRequestDto dto) {
        // 1. 문의 조회
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        // 2. 문의 조회
        if (inquiry.getStatus() != InquiryStatus.PENDING) {
            throw new IllegalArgumentException("완료된 문의는 수정할 수 없습니다.");
        }

        // 3. 수정(Entity에 update 메서드 필요)
        inquiry.update(dto.getTitle(), dto.getContent());

        // 4. 저장 및 변환
        return inquiryRepository.save(inquiry);
    }

    /**
     * 관리자 답변 작성
     */
    @Transactional
    public Inquiry reply(Long inquiryId, InquiryReplyRequestDto dto) {
        // 1. 문의 조회
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        // 2. 답변 작성(Entity 에 reply 메서드 필요)
        inquiry.reply(dto.getReply());

        // 3. 저장 및 반환
        return inquiryRepository.save(inquiry);
    }

    /**
     * 전체 문의 목록 조회(관리자)
     */

    public List<Inquiry> findAll() {
        return inquiryRepository.findAll();
    }

}
