# JDBC Session 저장소 (PostgreSQL)

## 요약

Spring Session JDBC를 사용하여 HTTP 세션을 PostgreSQL에 저장한다.
테이블은 Spring이 자동 생성하고, 앱 시작 시 unlogged 테이블로 전환한다.

## 결정 사항

- **구현 방식**: `spring-session-jdbc` 의존성 + auto-configuration
- **테이블 생성**: `spring.session.jdbc.initialize-schema=always` (자동)
- **테이블 타입**: 앱 시작 시 `ALTER TABLE ... SET UNLOGGED` (이미 unlogged면 skip)
- **트레이드오프**: 크래시 시 세션 유실 감수 (캐시와 동일)

## 변경 범위

1. `build.gradle.kts` — `spring-session-jdbc` 의존성 추가
2. `application.yaml` — session store-type, initialize-schema 설정
3. 새 파일: `SessionConfig.kt` — ApplicationRunner로 ALTER TABLE SET UNLOGGED 실행
