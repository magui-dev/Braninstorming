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
    private String content;
    private String purpose;

}
