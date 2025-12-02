package com.brainstorming.brainstorming_platform.global.security.jwt;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *  JWT 설정정보를 담는 클래스
 *  Application.yaml 의 jwt.* 설정을 자동으로 매핑
 *  ex)
 *  jwt :
 *    secret:"비밀키"
 *    expiration: 7200000
 *
 *  -> jwtProperties.getSecret() ="비밀키"
 *  -> jwtProperties.getExpiration() = 7200000
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt") //yaml에서 jwt.*읽기
public class JwtProperties {

    /**
     * JWT 서명에 사용할 비밀키
     *
     * application.yaml:
     * jwt:
     *   secret: "your-256-bit-secret..."
     *
     *   -최소 32자이상(HS256알고리즘)
     *   -공개금지, 운영환경에선 환경변수로 관리필요
     */
    private String secret;

    /**
     * JWT 토큰 만료 시간(밀리초)
     * jwt:
     *   expiration: 7200000 #2시간
     *   1초는 1000ms
     */
    private Long expiration;


}
