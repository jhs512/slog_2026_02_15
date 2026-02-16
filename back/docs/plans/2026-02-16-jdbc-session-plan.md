# JDBC Session 저장소 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Spring Session JDBC로 HTTP 세션을 PostgreSQL에 저장하고, 테이블을 unlogged로 전환한다.

**Architecture:** `spring-session-jdbc` auto-configuration이 `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` 테이블을 자동 생성한다. 앱 시작 시 `ApplicationRunner`가 해당 테이블이 unlogged인지 확인하고, 아니면 `ALTER TABLE ... SET UNLOGGED`를 실행한다.

**Tech Stack:** Spring Boot 4, Spring Session JDBC, PostgreSQL, Kotlin

---

### Task 1: spring-session-jdbc 의존성 추가

**Files:**
- Modify: `build.gradle.kts:30-51` (dependencies 블록)

**Step 1: 의존성 추가**

`build.gradle.kts`의 dependencies 블록에 추가:

```kotlin
implementation("org.springframework.session:spring-session-jdbc")
```

`spring-boot-starter-webmvc` 바로 아래에 넣는다.

**Step 2: Gradle sync 확인**

Run: `./gradlew dependencies --configuration runtimeClasspath | grep spring-session`
Expected: `spring-session-jdbc` 출력

**Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "spring-session-jdbc 의존성 추가"
```

---

### Task 2: application.yaml 세션 설정

**Files:**
- Modify: `src/main/resources/application.yaml` (spring 섹션)

**Step 1: 세션 설정 추가**

`application.yaml`의 `spring:` 하위에 추가 (spring.session):

```yaml
spring:
  session:
    jdbc:
      initialize-schema: always
```

Spring Boot 3+에서는 `store-type` 설정이 제거됨. classpath에 `spring-session-jdbc`만 있으면 자동 감지된다.

**Step 2: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "Spring Session JDBC 설정 추가"
```

---

### Task 3: SessionConfig — unlogged 테이블 전환

**Files:**
- Create: `src/main/kotlin/com/back/global/session/config/SessionConfig.kt`

**Step 1: SessionConfig 작성**

```kotlin
package com.back.global.session.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class SessionConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun sessionTableUnloggedRunner(dataSource: DataSource) = ApplicationRunner {
        dataSource.connection.use { conn ->
            val tables = listOf("spring_session", "spring_session_attributes")
            for (table in tables) {
                val isUnlogged = conn.prepareStatement(
                    """
                    SELECT relpersistence = 'u'
                    FROM pg_class
                    WHERE relname = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, table)
                    stmt.executeQuery().use { rs ->
                        rs.next() && rs.getBoolean(1)
                    }
                }

                if (!isUnlogged) {
                    conn.createStatement().use { stmt ->
                        stmt.execute("ALTER TABLE $table SET UNLOGGED")
                    }
                    log.info("테이블 {}을 unlogged로 전환했습니다.", table)
                }
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/back/global/session/config/SessionConfig.kt
git commit -m "앱 시작 시 세션 테이블 unlogged 전환"
```

---

### Task 4: 빌드 및 앱 시작 검증

**Step 1: 컴파일 확인**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

**Step 2: 앱 시작 + 테이블 확인**

앱을 시작하고, 로그에서 다음을 확인:
- Spring Session JDBC 초기화 로그
- "테이블 spring_session을 unlogged로 전환했습니다." 로그 (첫 실행 시)

**Step 3: DB에서 unlogged 확인**

```sql
SELECT relname, relpersistence
FROM pg_class
WHERE relname IN ('spring_session', 'spring_session_attributes');
```

Expected: 두 테이블 모두 `relpersistence = 'u'`

**Step 4: 재시작 시 skip 확인**

앱을 재시작하고, "unlogged로 전환" 로그가 출력되지 않는지 확인.
