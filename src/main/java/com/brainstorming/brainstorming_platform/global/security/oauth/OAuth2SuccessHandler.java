package com.brainstorming.brainstorming_platform.global.security.oauth;

import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.repository.UserRepository;
import com.brainstorming.brainstorming_platform.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Spring security 제공 성공핸들로 확장
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * OAuth 2.0 로그인 성공시 자동 호출 메서드
     * @param request  HTTP 요청
     * @param response  HTTP 응답
     * @param authentication  인증정보(로그인한 사용자 정보 포함)
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication/* 인증 정보 */) throws IOException, ServletException {
        /**
         * TODO 1-user정보가져오기, 2-JWT토큰 생성, 3- 프론트엔드로 리다이렉트(토큰포함)
         */

        log.info("OAuth 2.0 로그인 성공, 핸들러 실행");

        /**
         * 1 . OAuth2USer 가져오기
         */
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        log.info("OAuth2User정보:{}", oAuth2User.getAttributes());

        /**
         * 2 . 이메일로 DB에서 User 찾기
         */
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            log.error("이메일 정보 없음");
            //에러페이지로 리다이렉트
            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    "http://localhost:3000/login?error=email_required"
            );
            return;
        }

        log.info("사용자 이메일: {}", email);

        //DB에서 User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.error("DB에서 사용자를 찾을 수 없음: {}",email);
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });

        log.info("User 조회 성공 - userId: {}", user.getUserId());

        /**
         * 3 . JWT 토큰 생성
         */
        String token = jwtTokenProvider.createToken(user);

        log.info("JWT 토큰 생성 완료 - 길이: {}", token.length());

        /**
         * 4 . 프론트로 리다이렉트(토큰포함)
         */
        String targetUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth/callback")
                .queryParam("token",token)
                .build()
                .toUriString();

        log.info("리다이렉트 URL: {}", targetUrl);

        //리다이렉트 실행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);

        log.info("OAuth 로그인 처리 완료");
    }
}
