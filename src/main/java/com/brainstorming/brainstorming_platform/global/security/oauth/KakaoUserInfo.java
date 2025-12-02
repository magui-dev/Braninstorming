package com.brainstorming.brainstorming_platform.global.security.oauth;

import java.util.Map;

/**
 * Kakao 사용자 구현체
 * Kakao는 중첩 구조가 복잡
 * ID가 Long타입 -> String 변환 필요
 * email 선택동의 항목(null가능)
 */
public class KakaoUserInfo implements Oauth2UserInfo{

    // Kakao 에서 받은 사용자 정보(Json ->Map변환)
    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        // Kakao는 "id" 필드에 Long 타입으로 ID를 보냄
        // String으로 변환 필요!
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        // kakaosms 이메일이 "kakao_account" 안에 중첩되어있음
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            return null; // 동의 안한 경우
        }
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        // kakao는 닉네임이 "properties" 안에있음
        Map<String, Object> properties =
                (Map<String, Object>) attributes.get("properties");

        if (properties == null) {
            return null;
        }

        return (String) properties.get("nickname");
    }
}
