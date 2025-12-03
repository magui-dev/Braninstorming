package com.brainstorming.brainstorming_platform.domain.inquiry.entity;

import com.brainstorming.brainstorming_platform.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inquiries")
@Entity
@Getter
public class Inquiry extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryId;

    private Long userId;
    private String title;
    private String content;

    @Enumerated(EnumType.STRING)
    private InquiryStatus status;

    private String reply;

    /**
     * 비지니스 로직 메서드 추가
     * 관리자용 포함
     */
    /**
     * 문의 수정
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /**
     * 관리자 답변 작성
     */
    public void reply(String replyContent) {
        this.reply = replyContent;
        this.status = InquiryStatus.ANSWERED;
    }

    /**
     * 문의 종료
     */
    public void close() {
        this.status = InquiryStatus.CLOSED;
    }


}
