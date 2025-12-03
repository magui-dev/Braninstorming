package com.brainstorming.brainstorming_platform.global.security.jwt;

import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 담당
 * 1. 토큰생성
 * 2. 토큰 검증
 * 3. 토큰정보 추출
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * JWT 서명용  Secret Key 생성
     * application.yaml 의 secret 문자열을 SecretKey 객체로 변환함
     * @return SecretKey 객체
     */
    private SecretKey getSignKey() {
        //문자열 secret을 바이트 배열로 변환
        byte[] keyBytes = jwtProperties.getSecret()
                .getBytes(StandardCharsets.UTF_8);

        //바이트 배열을 SecretKey로 변환
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(User user) {

        log.info("JWT 토큰 생성 시작 - userId: {}, role: {}", user.getUserId(), user.getRole());

        //현재시간
        Date now = new Date();

        //만료 시간 = 현재시간 + expiration(밀리초)
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());

        log.info("토큰 만료 시간: {}", expiration);

        // JWT 토큰 생성
        String token = Jwts.builder()
                // Payload  설정
                .subject(String.valueOf(user.getUserId())) // "sub": "1"
                .claim("role", user.getRole().name())      // "role": "ADMIN" ← 추가!
                .issuedAt(now)                             // "iat": 현재시간
                .expiration(expiration)                    // "exp": 만료시간

                // Signature 설정
                .signWith(getSignKey())                    // 서명

                //토큰생성
                .compact();

        log.info("JWT 토큰 생성 완료 - 길이: {} 문자", token.length());
        return token;
    }

    public boolean validateToken(String token) {

        try {
            log.info(" JWT 토큰 검증 시작");

            //토큰 파싱 & 검증
            Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token);

            log.info(" JWT 토큰 검증 성공");
            return true;

        } catch (ExpiredJwtException e) {
            //만료된 토큰
            log.error("만료된 JWT 토큰 : {}", e.getMessage());
            return false;

        } catch (UnsupportedJwtException e) {
            //지원하지 않는 토큰
            log.error("지원하지 않는 JWT 토큰: {}", e.getMessage());
            return false;

        } catch (MalformedJwtException e) {
            // 형식이 잘못된 토큰
            log.error("잘못된 JWT 토큰: {}", e.getMessage());
            return false;

        } catch (SecurityException e) {
            // 잘못된 서명 토큰
            log.error("서명이 잘못된 JWT 토큰: {}", e.getMessage());
            return false;

        } catch (IllegalArgumentException e) {
            //빈토큰
            log.error("빈 JWT 토큰: {}", e.getMessage());
            return false;

        } catch (Exception e) {
            // 기타 예외
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     *  토큰 -> Claims -> subject -> userId
     * @param token  JWT
     * @return  사용자 Id (Long)
     */
    public Long getUserIdFromToken(String token) {

        log.info("JWT 토큰에서 userId 추출 시작");

        try {
            //토큰 파싱 & Claims 추출
            Claims claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload(); //Claims = payload

            //subject(userId 추출)
            String subject = claims.getSubject();
            Long userId = Long.valueOf(subject);

            log.info("userId 추출 성공: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("userId 추출 실패: {}", e.getMessage());
            throw new RuntimeException("토큰에서 사용자 정보를 추출할 수 없습니다. ", e);
        }
    }

    /**
     * JWT 토큰에서 role 추출
     */
    public String getRoleFromToken(String token) {
        log.info("JWT 토큰에서 role 추출 시작");

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String role = claims.get("role", String.class);
            log.info("role 추출 성공: {}", role);
            return role;
        } catch (Exception e) {
            log.error("role 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}
