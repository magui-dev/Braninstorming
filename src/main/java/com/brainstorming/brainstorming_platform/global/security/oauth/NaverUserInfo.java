package com.brainstorming.brainstorming_platform.global.security.oauth;

import java.util.Map;
import java.util.Objects;

/**
 * Naver OAuth 사용자 정보 구현체
 * naver는 모덴 데이터가 "response" 객체 안에있음
 * application.yaml 에서 user-name-attribute: response 설정한 이유
 */

public class NaverUserInfo implements Oauth2UserInfo {

    private final Map<String, Object> attributes;

    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        //naver는 "response"안에 id 존재
        Map<String, Object> response =
                (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }

        return (String) response.get("id");
    }

    @Override
    public String getEmail() {
        Map<String, Object> response =
                (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> response =
                (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("name");
    }
}
