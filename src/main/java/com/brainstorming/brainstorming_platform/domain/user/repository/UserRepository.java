package com.brainstorming.brainstorming_platform.domain.user.repository;

import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //이메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    //이메일 존재 여부 확인
    boolean existsByEmail(String email);

}
