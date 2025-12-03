package com.brainstorming.brainstorming_platform.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * - 정적 리소스 경로 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 등록
     * frontend 폴더를 정적 파일 경로로 추가
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // frontend 폴더를 정적 리소스로 서빙
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/static/",
                        "file:./frontend/"  // 프로젝트 루트의 frontend 폴더
                )
                .setCachePeriod(0); // 개발 중에는 캐시 비활성화
    }

    /**
     * 루트 경로(/) 접속 시 index.html로 리다이렉트
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
