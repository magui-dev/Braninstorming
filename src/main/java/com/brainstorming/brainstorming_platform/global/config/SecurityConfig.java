package com.brainstorming.brainstorming_platform.global.config;

import com.brainstorming.brainstorming_platform.global.security.oauth.CustomOAuth2UserService;
import com.brainstorming.brainstorming_platform.global.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // CustomOAuth2UserService 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 개발 단계에서는 모든 요청 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/","login","/oauth2/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // H2 콘솔 접근 허용
                        .anyRequest().permitAll()  // 모든 요청 허용 (임시)
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .defaultSuccessUrl("/",true) // 로그인 성공후 이동
                        .userInfoEndpoint(userInfo->userInfo
                                .userService(customOAuth2UserService)  //서비스계층 등록
                        )
                )
                // CSRF 비활성화 (REST API)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .disable()
                )
                // H2 콘솔을 위한 Frame 옵션 비활성용
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}
