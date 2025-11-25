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


}
