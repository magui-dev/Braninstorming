package com.brainstorming.brainstorming_platform.global.security.jwt;


import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * JWT 인증 필터
 * <p>
 * 모든 API 요청을 가로채서 :
 * 1. Authorization 헤더에서 JWT 토큰추출
 * 2. 토큰 검증
 * 3. 유효하면 spring security에 인증 정보 등록
 */
@Slf4j
@Component
@RequiredArgsConstructor
// OncePerRequestFilter = 요청당 딱 1번만 실행됨  [doFilterInternal() 메서드를 오버라이드해야 함]
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            //1 . Authorization 헤더에서 JWT 추출
            String token = extractToken(request);

            // 2. 토큰이 있고 유효한지 확ㅇ니
            if (token != null && jwtTokenProvider.validateToken(token)) {

                log.info("JWT 토큰 발견 및 검증 성공");

                // 3. 토큰에서 userId 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                log.info("userId 추출: {}", userId);

                // 4. DB에서 User 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

                log.info("User 조회 성공: {}", user.getEmail());

                // 5. Spring security에 인증 정보 등록
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,                   //principal(주체)
                                null,                   //credentials( 비멀번호, 필요없음)
                                user.getAuthorities()   // 권한
                        );

                // 6. 요청 정보 추가( IP, 세션 등)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. SecurityContext에 인증정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("Spring Security 인증 완료");
            }
        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
        }

        // 8. 다음 필터로 넘어가기
        filterChain.doFilter(request, response);
    }


    /**
     * HTTP 요청 헤더에서 JWT 토큰 추출 (Bearer 제거)
     */
    private String extractToken(HttpServletRequest request) {
        // Authorization 헤더 가져오기
        String bearerToken = request.getHeader("Authorization");

        log.info("Authorization 헤더: {}",
                bearerToken != null ? bearerToken.substring(0, Math.min(20, bearerToken.length())) + "..." : "없음");

        //"Bearer" 로 시작하는지 확인
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            //"Bearer" 제거하고 토큰만 반환
            return bearerToken.substring(7);
        }
        return null;
    }
}
