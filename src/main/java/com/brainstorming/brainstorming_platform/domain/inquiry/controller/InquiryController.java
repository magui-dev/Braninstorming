package com.brainstorming.brainstorming_platform.domain.inquiry.controller;

import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryReplyRequestDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryRequestDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryResponseDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.dto.InquiryUpdateRequestDto;
import com.brainstorming.brainstorming_platform.domain.inquiry.entity.Inquiry;
import com.brainstorming.brainstorming_platform.domain.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의사항 저장
     */
    @PostMapping
    public ResponseEntity<InquiryResponseDto> createInquiry(@RequestBody InquiryRequestDto requestDto) {
        // 1 Dto -> Entity 변환
        Inquiry inquiry = requestDto.toEntity();
        // 2 Service 호출
        Inquiry savedInquiry = inquiryService.save(inquiry);
        // 3 entity -> ResponseDto 변환
        InquiryResponseDto responseDto = InquiryResponseDto.from(savedInquiry);
        // 4 응답
        return ResponseEntity.ok(responseDto);
    }

    /**
     * ID 별 문의사항 찾기
     */

    @GetMapping("/{id}")
    public ResponseEntity<InquiryResponseDto> getInquiry(@PathVariable("id") Long inquiryId) {
        // 1 service 호출
        Inquiry findInquiry = inquiryService.findById(inquiryId);
        // 2 dto 변환
        InquiryResponseDto responseDto = InquiryResponseDto.from(findInquiry);
        // 3 응답
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Id로 유저 모든 문의 찾기
     */
    @GetMapping
    public ResponseEntity<List<InquiryResponseDto>> getInquiries(@RequestParam Long userId) {
        // 1 service 호출
        List<Inquiry> inquiries = inquiryService.findByUserId(userId);
        // 2 dto 변환
        List<InquiryResponseDto> inquiryResponseDtos = inquiries.stream()
                .map(InquiryResponseDto::from)
                .toList();
        // 3 응답
        return ResponseEntity.ok(inquiryResponseDtos);
    }

    /**
     * 문의사항 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable("id") Long inquiryId) {
        // 1 service 호출
        inquiryService.delete(inquiryId);
        // 2 응답
        return ResponseEntity.noContent().build();
    }

    /**
     * 문의 사항 수정(사용자)
     */
    @PutMapping("/{id}")
    public ResponseEntity<InquiryResponseDto> updateInquiry(
            @PathVariable("id") Long inquiryId,
            @RequestBody InquiryUpdateRequestDto requestDto
    ) {
        //status 가 PENDING일 때만 수정 가능
        Inquiry updatedInquiry = inquiryService.update(inquiryId, requestDto);
        return ResponseEntity.ok(InquiryResponseDto.from(updatedInquiry));
    }

    /**
     * 관리자 답변 작성
     */
    @PutMapping("/{id}/reply")
    public ResponseEntity<InquiryResponseDto> replyInquiry(
            @PathVariable("id") Long inquiryId,
            @RequestBody InquiryReplyRequestDto requestDto
    ) {
        Inquiry repliedInquiry = inquiryService.reply(inquiryId, requestDto);
        return ResponseEntity.ok(InquiryResponseDto.from(repliedInquiry));
    }

    /**
     * 전체 문의 목록(관리자)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<InquiryResponseDto>> getAllInquiries() {
        List<Inquiry> inquiries = inquiryService.findAll();
        return ResponseEntity.ok(inquiries.stream()
                .map(InquiryResponseDto::from)
                .toList());
    }
}
