package com.brainstorming.brainstorming_platform.global.config;

import com.brainstorming.brainstorming_platform.domain.user.entity.LoginProvider;
import com.brainstorming.brainstorming_platform.domain.user.entity.MyRole;
import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 초기 데이터 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        createAdminIfNotExists();
    }

    /**
     * 관리자 계정이 없으면 자동 생성
     */
    private void createAdminIfNotExists() {
        // ADMIN 권한을 가진 사용자가 이미 있는지 확인
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == MyRole.ADMIN);

        if (!adminExists) {
            User admin = User.builder()
                    .email(adminEmail)
                    .username(adminUsername)
                    .provider(LoginProvider.LOCAL)
                    .providerId("ADMIN_ACCOUNT")
                    .role(MyRole.ADMIN)
                    .build();

            userRepository.save(admin);

            log.info("========================================");
            log.info("✅ 초기 관리자 계정 생성 완료!");
            log.info("========================================");
            log.info("📧 이메일: {}", adminEmail);
            log.info("👤 이름: {}", adminUsername);
            log.info("🔑 비밀번호: {} (현재 OAuth만 지원)", adminPassword);
            log.info("========================================");
            log.info("🔐 관리자 계정 로그인 방법:");
            log.info("1. Google/Kakao/Naver로 로그인");
            log.info("2. MySQL에서 해당 계정 role을 ADMIN으로 변경");
            log.info("========================================");
        } else {
            log.info("✅ 관리자 계정이 이미 존재합니다.");
        }
    }
}
