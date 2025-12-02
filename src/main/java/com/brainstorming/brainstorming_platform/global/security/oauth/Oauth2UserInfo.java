package com.brainstorming.brainstorming_platform.global.security.oauth;

/**
 * OAuth 2.0 제공자별 사용자 정보 추상화 인터페이스
 * Google, Kakao, Naver의 서로 다른 응답형식을 통일
 */


public interface Oauth2UserInfo {

    /**
     *  OAuth 제공자(google, kakao, naver)
     *  ex) GoogleUserInfo -> "google"
     */
    String getProvider();

    /**
     * 제공자가 제공하는 사용자 고유 ID
     * 이메일은 변경가능하지만 ID는 불변
     * DB에서 사용자를 찾을때 "제공자+ID" 조합사용
     */
    String getProviderId();

    /**
     * 사용자 이메일 없이는 가입불가처리 필요
     */
    String getEmail();

    /**
     * 사용자 이름
     * google(실명),kakao(닉네임),naver(실명)
     */
    String getName();


}
