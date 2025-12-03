package com.brainstorming.brainstorming_platform.global.security.oauth;


import com.brainstorming.brainstorming_platform.domain.user.entity.LoginProvider;
import com.brainstorming.brainstorming_platform.domain.user.entity.MyRole;
import com.brainstorming.brainstorming_platform.domain.user.entity.User;
import com.brainstorming.brainstorming_platform.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth 2.0로그인 서비스
 * Spring Security 가 자동으로 호출하는 핵심 서비스
 * <p>
 * 역할 :
 * 1.Oauth 제공자에서 사용자 정보받기(구글,네이버,카카오)
 * 2. DB에서 사용자 찾기
 * 3. 없으면 회원가입, 있으면 업뎃
 * 4. Spring Security에 사용자 정보 전달
 */

@Slf4j
@Service
@RequiredArgsConstructor // final 필드 생성자 자동생성
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    //DB접근용 리파지토리
    private final UserRepository userRepository;

    /**
     * Spring Security 가 자동으로 호출하는 메서드
     * @param userRequest     요청정보
     *                        -제공자 (google,kakao,naver)
     *                        -Access Token
     *                        -Client 정보
     * @return                로그인된 사용자 정보
     * @throws OAuth2AuthenticationException  인증 실패시
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info(" OAuth2.0 로그인 시작");

        /**
         * 1 . 부모 클래스의 기능으로 Oauth 사용자 정보 가져오기
         */
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("Oauth 제공자 에서 사용자 정보 받음 : {}", oAuth2User.getAttributes());

        /**
         * 2 . 어떤 제공자인지 확인
         */
        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        log.info(" OAuth제공자 : {}", registrationId);

        /**
         * 3 . 제공자 별로 사용자 정보 파싱 // getOAuth2USerInfo 메서드생성
         */
        Oauth2UserInfo userInfo = getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        log.info(" 파싱된 사용자 정보 - 이메일 : {}, 이름: {} ",
                userInfo.getEmail(), userInfo.getName());

        /**
         * 4 . 이메일 확인(필수)
         */
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.error(" 이메일 정보 없음 - 로그인 불가 " );
            throw new OAuth2AuthenticationException(
                    "이메일을 찾을 수 없습니다. 이메일 제공에 동의해 주세요"
            );
        }

        /**
         * 5 . DB에서 사용자 찾기 or 생성
         *  saveOrUpdate 메서드 생성
         */
        User user = saveOrUpdate(userInfo);

        log.info(" 사용자 처리 완료 - ID : {}, 이메일 : {}",
                user.getUserId(), user.getEmail());

        /**
         * 6 . Spring Security 가 사용할 반환 객체 변환
         * Provider 원본 데이터 말고, 우리가 파싱한 User 정보를 담아서 반환
         */
        
        // attributes에 우리 User 정보 추가
        Map<String, Object> modifiedAttributes = new java.util.HashMap<>(oAuth2User.getAttributes());
        modifiedAttributes.put("userId", user.getUserId());
        modifiedAttributes.put("email", user.getEmail());
        modifiedAttributes.put("username", user.getUsername());
        modifiedAttributes.put("provider", user.getProvider().name());
        
        // nameAttributeKey 가져오기 (Provider마다 다름: google="sub", kakao="id", naver="id")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        
        log.info("nameAttributeKey: {}", userNameAttributeName);
        
        // DefaultOAuth2User로 반환 (우리 정보 포함)
        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                java.util.Collections.singleton(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ),
                modifiedAttributes,
                userNameAttributeName
        );
    }

    /**
     *
     * @param registrationId   google,kakao,naver
     * @param attributes   attribute Oauth 제공자가 준 사용자정보
     * @return 통일된 인터페이스 UserInfo 객체
     */
    private Oauth2UserInfo getOAuth2UserInfo(
            String registrationId,
            Map<String, Object> attributes) {

        log.info(" {} UserInfo 객체 생성 중 ...", registrationId);

        //switch로 제공자 구분
        return switch (registrationId.toLowerCase()) {
            case "google" -> {
                log.info("-> GoogleUserInfo 생성");
                yield new GoogleUserInfo(attributes);
            }
            case "kakao" -> {
                log.info("-> KakaoUserInfo 생성");
                yield new KakaoUserInfo(attributes);
            }
            case "naver" -> {
                log.info("-> NaverUserInfo 생성");
                yield new NaverUserInfo(attributes);
            }
            default -> {
                log.error(" 지원하지 않는 OAuth 제공자 : {}", registrationId);
                throw new OAuth2AuthenticationException(
                        "지원하지 않는 소셜 로그인 입니다." + registrationId
                );
            }
        };
    }

    /**
     * 사용자 DB에 저장하거나 업데이트
     * @param userInfo   OAuth 에서 받은 사용자 정보
     * @return  저장된 User 엔티티
     */
    private User saveOrUpdate(Oauth2UserInfo userInfo) {

        log.info(" DB 처리 시작 - 이메일 : {} ", userInfo.getEmail());
        //String -> LoginProvider 변환
        LoginProvider provider = LoginProvider.valueOf(
                userInfo.getProvider().toUpperCase()
        );

        //DB에서 사용자 찾기
        //provider+providerId 조합으로 찾음 (이메일은 변경가능)
        User user = userRepository.findByProviderAndProviderId(
                provider,
                userInfo.getProviderId()
        )
                .map(existingUser -> {
                    log.info(" 기존 사용자 업데이트 - ID : {}", existingUser.getUserId());
                    existingUser.updateOAuthInfo(
                            userInfo.getName(),
                            userInfo.getEmail()
                    );
                    return existingUser;
                })
                .orElseGet(()-> {
                    //신규 사용자- 회원가입
                    log.info(" 신규 사용자 생성");
                    return User.builder()
                            .email(userInfo.getEmail())
                            .username(userInfo.getName())
                            .provider(LoginProvider.valueOf(
                                    userInfo.getProvider().toUpperCase())) // Enum
                            .providerId(userInfo.getProviderId())
                            .role(MyRole.USER)
                            .build();
                });

        User saveUser = userRepository.save(user);
        log.info("DB 저장 완료 - User ID : {}", saveUser.getUserId());

        return saveUser;

    }



}
