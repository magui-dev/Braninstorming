package com.brainstorming.brainstorming_platform.domain.user.service;

import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 저장 (OAuth 자동 가입 시 사용)
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * 아이디 찾기
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다."));
    }

    /**
     * 이메일로 아이디 찾기
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 아이디 삭제
     */
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }
}
