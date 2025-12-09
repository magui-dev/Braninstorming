package com.brainstorming.brainstorming_platform.domain.idea.entity;

import com.brainstorming.brainstorming_platform.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ideas")
@Getter
public class Idea extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ideaId;

    private Long userId;

    /**
     * 아이디어 제목, 아이디어 내용, 아이디어 요구사항
     */
    private String title;
    
    @Column(columnDefinition = "TEXT")  // ✅ 긴 내용 저장 가능
    private String content;
    
    private String purpose; //브레인스토밍 프로세스 Q1의 답변을 저장하는 용도

    //비로그인 사용자 임시 저장용 세션 ID
    private String guestSessionId;

}
