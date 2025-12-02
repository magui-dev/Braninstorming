package com.brainstorming.brainstorming_platform.global.security.oauth;

import java.util.Map;

/**
 * Google OAuth 사용자 정보 구현체
 */

public class GoogleUserInfo implements Oauth2UserInfo {

    // Google 에서 받은 사용자 정보(Json ->Map변환)
    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google"; //고정값
    }

    @Override
    public String getProviderId() {
        // google은 "sub"필드에 사용자 ID를 담아서 보냄
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        // google은 "email"필드에 이메일을 담아서 보냄
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        // google은 "name"필드에 이름을 담아서 보냄
        return (String) attributes.get("name");
    }

}
