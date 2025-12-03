package com.brainstorming.brainstorming_platform.domain.user.controller;

import com.brainstorming.brainstorming_platform.domain.user.dto.UserResponseDto;
import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
/**
 * 현재 로그인한 사용자 정보조회
 * JWT 인증 필요
 */
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal User user) {

        log.info("현재 사용자 정보조회 - userId: {}", user.getUserId());

        UserResponseDto responseDto = UserResponseDto.from(user);
        return ResponseEntity.ok(responseDto);
    }
}
