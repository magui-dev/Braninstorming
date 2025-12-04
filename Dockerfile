# ==================================================
# Spring Boot Dockerfile
# ==================================================

# 1단계: Gradle로 JAR 파일 빌드
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle 설정 파일 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 코드 복사
COPY src ./src

# JAR 빌드 (테스트 스킵)
RUN gradle build -x test --no-daemon

# 2단계: 실행용 경량 이미지
FROM eclipse-temurin:17-jdk

WORKDIR /app

# 1단계에서 빌드한 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
