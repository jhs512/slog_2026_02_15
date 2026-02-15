# PostgreSQL Unlogged Table Cache 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Spring CacheManager/Cache 인터페이스를 구현하여 PostgreSQL unlogged 테이블(JSONB)을 캐시 저장소로 사용한다.

**Architecture:** `AbstractCacheManager`를 확장한 `PgCacheManager`가 cache name별로 `PgCache` 인스턴스를 생성한다. `PgCache`는 `JdbcTemplate`으로 `cache_store_unlogged` 테이블에 CRUD를 수행한다. Jackson `ObjectMapper`(타입 정보 포함)로 JSONB 직렬화.

**Tech Stack:** Spring Boot 4, Spring Cache Abstraction, JdbcTemplate, PostgreSQL (JSONB, UNLOGGED TABLE), Jackson

---

### Task 1: DDL — cache_store_unlogged 테이블

**Files:**
- Modify: `src/main/resources/schema.sql`

**Step 1: schema.sql에 테이블 DDL 추가**

`schema.sql` 끝에 추가:

```sql
CREATE TABLE IF NOT EXISTS cache_store_unlogged (
    cache_key   VARCHAR(512) PRIMARY KEY,
    value       JSONB NOT NULL,
    expired_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cache_store_expired_at ON cache_store_unlogged (expired_at);
```

참고: `_unlogged` suffix이므로 `CustomPostgreSQLDialect`가 자동으로 `CREATE UNLOGGED TABLE`로 변환한다.
단, `schema.sql`은 Hibernate DDL이 아니라 Spring SQL init이므로 직접 `UNLOGGED`를 써야 한다.
→ JPA 엔티티로 관리하지 않으므로 schema.sql에서 직접 `CREATE UNLOGGED TABLE`로 작성.

수정:

```sql
CREATE UNLOGGED TABLE IF NOT EXISTS cache_store_unlogged (
    cache_key   VARCHAR(512) PRIMARY KEY,
    value       JSONB NOT NULL,
    expired_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cache_store_expired_at ON cache_store_unlogged (expired_at);
```

**Step 2: 앱 실행하여 테이블 생성 확인**

Run: `./gradlew bootRun` 후 DB에서 `\d cache_store_unlogged` 확인
Expected: 테이블 존재, relpersistence = 'u' (unlogged)

**Step 3: Commit**

```
feat: add cache_store_unlogged table DDL
```

---

### Task 2: PgCache — Cache 인터페이스 구현

**Files:**
- Create: `src/main/kotlin/com/back/global/cache/PgCache.kt`

**Step 1: PgCache 구현**

```kotlin
package com.back.global.cache

import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import java.time.Duration
import java.util.concurrent.Callable

class PgCache(
    private val cacheName: String,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val ttl: Duration,
) : Cache {

    companion object {
        // 타입 정보를 포함하는 ObjectMapper 생성
        fun createTypingObjectMapper(source: ObjectMapper): ObjectMapper {
            return source.rebuild()
                .activateDefaultTyping(
                    DefaultBaseTypeLimitingValidator(),
                )
                .build()
        }
    }

    private fun cacheKey(key: Any): String = "$cacheName::$key"

    override fun getName(): String = cacheName

    override fun getNativeCache(): Any = this

    override fun get(key: Any): ValueWrapper? {
        val json = jdbcTemplate.query(
            "SELECT value FROM cache_store_unlogged WHERE cache_key = ? AND expired_at > now()",
            { rs, _ -> rs.getString("value") },
            cacheKey(key),
        ).firstOrNull() ?: return null

        val value = objectMapper.readValue(json, Any::class.java)
        return SimpleValueWrapper(value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        val wrapper = get(key) ?: return null
        return wrapper.get() as T?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        val wrapper = get(key)
        if (wrapper != null) return wrapper.get() as T?

        val value = valueLoader.call()
        put(key, value)
        return value
    }

    override fun put(key: Any, value: Any?) {
        if (value == null) {
            evict(key)
            return
        }

        val json = objectMapper.writeValueAsString(value)

        jdbcTemplate.update(
            """
            INSERT INTO cache_store_unlogged (cache_key, value, expired_at)
            VALUES (?, ?::jsonb, now() + ?::interval)
            ON CONFLICT (cache_key)
            DO UPDATE SET value = EXCLUDED.value, expired_at = EXCLUDED.expired_at
            """.trimIndent(),
            cacheKey(key),
            json,
            "${ttl.seconds} seconds",
        )
    }

    override fun evict(key: Any) {
        jdbcTemplate.update(
            "DELETE FROM cache_store_unlogged WHERE cache_key = ?",
            cacheKey(key),
        )
    }

    override fun clear() {
        jdbcTemplate.update(
            "DELETE FROM cache_store_unlogged WHERE cache_key LIKE ?",
            "$cacheName::%",
        )
    }
}
```

**Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```
feat: implement PgCache (Spring Cache interface over PostgreSQL)
```

---

### Task 3: PgCacheManager — CacheManager 구현

**Files:**
- Create: `src/main/kotlin/com/back/global/cache/PgCacheManager.kt`

**Step 1: PgCacheManager 구현**

```kotlin
package com.back.global.cache

import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class PgCacheManager(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val ttl: Duration,
) : AbstractCacheManager() {

    override fun loadCaches(): Collection<Cache> = emptyList()

    override fun getMissingCache(name: String): Cache {
        return PgCache(name, jdbcTemplate, objectMapper, ttl)
    }
}
```

`loadCaches()`는 빈 리스트 반환 (사전 정의된 캐시 없음). `getMissingCache()`가 동적으로 cache name에 대해 `PgCache` 인스턴스를 생성한다. `AbstractCacheManager`가 내부적으로 ConcurrentMap에 캐싱하므로 같은 name에 대해 한 번만 생성.

**Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```
feat: implement PgCacheManager
```

---

### Task 4: PgCacheConfig — 빈 등록 및 캐시 활성화

**Files:**
- Create: `src/main/kotlin/com/back/global/cache/config/PgCacheConfig.kt`
- Modify: `src/main/resources/application.yaml`

**Step 1: application.yaml에 TTL 설정 추가**

`custom:` 블록에 추가:

```yaml
custom:
  cache:
    ttl-seconds: 3600
```

**Step 2: PgCacheConfig 구현**

```kotlin
package com.back.global.cache.config

import com.back.global.cache.PgCache
import com.back.global.cache.PgCacheManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
@EnableCaching
class PgCacheConfig {

    @Bean
    fun cacheManager(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        @Value("\${custom.cache.ttl-seconds}") ttlSeconds: Long,
    ): CacheManager {
        val typingMapper = PgCache.createTypingObjectMapper(objectMapper)
        return PgCacheManager(jdbcTemplate, typingMapper, Duration.ofSeconds(ttlSeconds))
    }
}
```

**Step 3: 앱 실행하여 빈 등록 확인**

Run: `./gradlew bootRun`
Expected: 정상 기동, `cacheManager` 빈 등록 로그 없이 에러 없음

**Step 4: Commit**

```
feat: add PgCacheConfig with @EnableCaching
```

---

### Task 5: 통합 테스트

**Files:**
- Create: `src/test/kotlin/com/back/global/cache/PgCacheIntegrationTest.kt`

**Step 1: 테스트 작성**

```kotlin
package com.back.global.cache

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class PgCacheIntegrationTest {

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM cache_store_unlogged")
    }

    @Test
    fun `put and get`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        val result = cache.get("key1", String::class.java)

        assertEquals("hello", result)
    }

    @Test
    fun `get returns null for missing key`() {
        val cache = cacheManager.getCache("test")!!

        val result = cache.get("nonexistent")

        assertNull(result)
    }

    @Test
    fun `evict removes entry`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        cache.evict("key1")
        val result = cache.get("key1")

        assertNull(result)
    }

    @Test
    fun `clear removes all entries for cache name`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "a")
        cache.put("key2", "b")
        cache.clear()

        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
    }

    @Test
    fun `clear does not affect other cache names`() {
        val cache1 = cacheManager.getCache("cache1")!!
        val cache2 = cacheManager.getCache("cache2")!!

        cache1.put("key1", "a")
        cache2.put("key1", "b")

        cache1.clear()

        assertNull(cache1.get("key1"))
        assertEquals("b", cache2.get("key1", String::class.java))
    }

    @Test
    fun `expired entry returns null`() {
        val cache = cacheManager.getCache("test")!!
        cache.put("key1", "hello")

        // 강제로 만료시키기
        jdbcTemplate.update(
            "UPDATE cache_store_unlogged SET expired_at = now() - interval '1 second' WHERE cache_key = ?",
            "test::key1",
        )

        assertNull(cache.get("key1"))
    }

    @Test
    fun `put with complex object`() {
        val cache = cacheManager.getCache("test")!!

        val data = mapOf("name" to "test", "count" to 42)
        cache.put("complex", data)

        val result = cache.get("complex", Map::class.java)
        assertEquals("test", result?.get("name"))
        assertEquals(42, result?.get("count"))
    }
}
```

**Step 2: 테스트 실행**

Run: `./gradlew test --tests "com.back.global.cache.PgCacheIntegrationTest"`
Expected: 모든 테스트 PASS

**Step 3: Commit**

```
test: add PgCache integration tests
```

---

### Task 6: @EnableCaching를 BackApplication에서 제거 (중복 방지 확인)

**Files:**
- Verify: `src/main/kotlin/com/back/BackApplication.kt`

**Step 1: 확인**

`BackApplication.kt`에 `@EnableCaching`이 없는지 확인. `PgCacheConfig`에만 있어야 한다. 현재 없으므로 변경 불필요.

**Step 2: 전체 테스트 실행**

Run: `./gradlew test`
Expected: 모든 테스트 PASS

**Step 3: Commit (변경이 있는 경우만)**

---

## 참고: pg_cron 설정

운영 DB에서 수동 설정 필요 (DDL이 아니라 DB 확장):

```sql
SELECT cron.schedule(
    'clean-expired-cache',
    '*/5 * * * *',
    $$DELETE FROM cache_store_unlogged WHERE expired_at <= now()$$
);
```

이는 코드가 아니라 인프라 설정이므로 이 구현 계획에서는 제외한다.
