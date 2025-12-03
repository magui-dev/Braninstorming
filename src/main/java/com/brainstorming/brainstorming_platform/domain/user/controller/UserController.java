package com.brainstorming.brainstorming_platform.domain.user.controller;

import com.brainstorming.brainstorming_platform.domain.user.dto.UserRequestDto;
import com.brainstorming.brainstorming_platform.domain.user.dto.UserResponseDto;
import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    //ID 생성
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto requestDto) { //ResponseEntity > HTTP응답객체
        // 1 Dto->entity 변환
        User user = requestDto.toEntity();
        // 2 Service 호출
        User savedUser = userService.save(user);
        // 3 Entity -> ResponseDto변환
        UserResponseDto responseDto = UserResponseDto.from(savedUser);
        // 4 응답
        return ResponseEntity.ok(responseDto);
    }

    //ID로 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        // 1 Service 호출
        User user = userService.findById(id);
        // 2 Entity -> Dto 변환
        UserResponseDto responseDto = UserResponseDto.from(user);
        // 3 응답
        return ResponseEntity.ok(responseDto);
    }

    //Email로 조회
    @GetMapping("/email")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam String email) {
        // 1 Service 호출
        Optional<User> userOptional = userService.findByEmail(email);
        // 2 Optional 처리
        User user = userOptional.orElseThrow(
                () -> new RuntimeException("해당 이메일의 사용자가 없습니다.")
        );


        // 3 Dto변환 및 응답
        return ResponseEntity.ok(UserResponseDto.from(user));
    }

    //회원탈퇴
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // 1 Service 호출
        userService.delete(id);
        // 2 응답 noContent() 204 기대
        return ResponseEntity.noContent().build();
    }
}
