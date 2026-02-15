# slog

## 기술 스택

- Kotlin 2.2 / Java 24
- Spring Boot 4.0
- Spring Data JPA + QueryDSL 7.1
- PostgreSQL
- Virtual Threads

## UNLOGGED 테이블

WAL 로깅을 생략하여 쓰기 성능을 높인다. 크래시 시 데이터 유실 가능.

- **dev/prod**: 테이블명이 `_unlogged`로 끝나는 테이블만 UNLOGGED로 생성 (`CustomPostgreSQLDialect`)
- **test**: 모든 테이블을 UNLOGGED로 생성 (`CustomTestPostgreSQLDialect`)

## ID 생성

애플리케이션에서 PostgreSQL 시퀀스(`{entity}_id_seq`)를 직접 호출하여 ID를 미리 채번한다. `Persistable`을 구현하여 `isNew()`로 신규 여부를 판단.

- `BaseEntity` — ID + `Persistable` + `attrCache`
- `BaseTime` — `BaseEntity` + `createdAt`/`modifiedAt` 감사. `isNew()`는 `createdAt` 초기화 여부로 판단
- `IdSource` — 엔티티 companion object에 구현. `newId()`로 채번

## Bounded Context 패키지 구조

```
boundedContexts/{context}/
├── app/       # Facade (유스케이스 조합)
├── domain/    # 엔티티
├── in/        # Controller, InitData
└── out/       # Repository
```

## 개발 환경 실행

```bash
# DB 실행
cd back/devInfra && docker compose up -d

# .env 설정
cp back/.env.default back/.env

# 앱 실행
cd back && ./gradlew bootRun
```

## 프로젝트 구조

```
back/
├── devInfra/
│   └── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── kotlin/com/back/
│   │   │   ├── BackApplication.kt
│   │   │   ├── boundedContexts/
│   │   │   │   └── member/
│   │   │   │       ├── app/MemberFacade.kt
│   │   │   │       ├── domain/Member.kt
│   │   │   │       ├── in/MemberNotProdInitData.kt
│   │   │   │       └── out/
│   │   │   │           ├── MemberRepository.kt
│   │   │   │           ├── MemberRepositoryCustom.kt
│   │   │   │           └── MemberRepositoryImpl.kt
│   │   │   ├── global/
│   │   │   │   ├── app/app/AppFacade.kt
│   │   │   │   └── jpa/
│   │   │   │       ├── config/
│   │   │   │       │   ├── CustomPostgreSQLDialect.kt
│   │   │   │       │   └── JpaConfig.kt
│   │   │   │       └── domain/
│   │   │   │           ├── BaseEntity.kt
│   │   │   │           ├── BaseTime.kt
│   │   │   │           └── IdSource.kt
│   │   │   └── standard/util/
│   │   │       ├── IdGenerator.kt
│   │   │       └── QueryDslUtil.kt
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-test.yaml
│   │       └── schema.sql
│   └── test/
│       └── kotlin/com/back/
│           ├── BackApplicationTests.kt
│           └── global/jpa/config/
│               └── CustomTestPostgreSQLDialect.kt
├── build.gradle.kts
└── settings.gradle.kts
```
