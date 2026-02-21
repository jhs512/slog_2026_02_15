# 첫 번째 스테이지: 빌드 스테이지
FROM gradle:jdk24-graal AS builder

WORKDIR /app

COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN gradle dependencies --no-daemon

COPY .env .
COPY src src

RUN gradle build --no-daemon

# 두 번째 스테이지: 실행 스테이지
FROM container-registry.oracle.com/graalvm/jdk:24

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/.env .env

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]