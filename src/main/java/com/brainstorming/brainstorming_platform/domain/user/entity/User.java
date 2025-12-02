package com.brainstorming.brainstorming_platform.domain.user.entity;

import com.brainstorming.brainstorming_platform.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Getter
@Builder
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

    public void updateOAuthInfo(String name, String email) {
        this.username = name;
        this.email = email;
    }



}
