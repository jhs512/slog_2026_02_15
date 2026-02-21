# 빌드 스테이지: jangka512/pgj 기반 (PGroonga + PostGIS + pgvector 포함)
FROM jangka512/pgj:latest AS builder

# JDK 24 + Gradle 설치 (toolchain languageVersion=24에 맞춤)
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip \
 && curl -sL https://download.java.net/java/GA/jdk24/1f9ff9062db4449d8ca828c504ffae90/36/GPL/openjdk-24_linux-x64_bin.tar.gz | tar xz -C /opt \
 && ln -s /opt/jdk-24 /opt/java \
 && curl -sL https://services.gradle.org/distributions/gradle-9.3.0-bin.zip -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && ln -s /opt/gradle-9.3.0 /opt/gradle \
 && rm /tmp/gradle.zip \
 && apt-get purge -y unzip && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java
ENV GRADLE_HOME=/opt/gradle
ENV PATH="${JAVA_HOME}/bin:${GRADLE_HOME}/bin:${PATH}"

WORKDIR /app

COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN gradle dependencies --no-daemon

COPY .env .
COPY src src

# PG 초기화 → 시작 → 테스트 DB 생성 → 빌드(테스트 포함) → PG 종료
ENV PGDATA=/var/lib/postgresql/data
RUN set -e \
 && mkdir -p "$PGDATA" && chown postgres:postgres "$PGDATA" \
 && su postgres -c "initdb -D $PGDATA --auth=trust" \
 && su postgres -c "pg_ctl -D $PGDATA start -o '-c listen_addresses=localhost' -w" \
 && su postgres -c "psql -c \"ALTER USER postgres PASSWORD 'lldj123414';\"" \
 && su postgres -c "psql -c \"CREATE DATABASE slog_test;\"" \
 && gradle build -x test --no-daemon \
 && su postgres -c "pg_ctl -D $PGDATA stop -w"

# 실행 스테이지
FROM container-registry.oracle.com/graalvm/jdk:24

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/.env .env

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]