package com.brainstorming.brainstorming_platform.domain.user.entity;

import com.brainstorming.brainstorming_platform.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

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

    /**
     * Spring Security가 권한을 확인할 때 사용
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //role을 Spring Security 권한 형식으로 변환
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }



}
