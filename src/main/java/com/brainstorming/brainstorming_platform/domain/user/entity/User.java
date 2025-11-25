package com.brainstorming.brainstorming_platform.domain.user.entity;

import com.brainstorming.brainstorming_platform.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String email;
    private String username;

    @Enumerated(EnumType.STRING)
    private LoginProvider provider;

    private String providerId;
    @Enumerated(EnumType.STRING)
    private MyRole role;



}
