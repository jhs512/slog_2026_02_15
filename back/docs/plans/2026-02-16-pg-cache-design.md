# PostgreSQL Unlogged Table Cache 설계

## 개요

Spring `CacheManager`/`Cache` 인터페이스를 구현하여 PostgreSQL unlogged 테이블(JSONB)을 캐시 저장소로 사용한다.
`@Cacheable`, `@CacheEvict`, `@CachePut` 표준 어노테이션을 그대로 사용할 수 있으며, 나중에 Redis 등으로 교체 가능하다.

## 테이블

```sql
CREATE UNLOGGED TABLE cache_store_unlogged (
    cache_key   VARCHAR(512) PRIMARY KEY,
    value       JSONB NOT NULL,
    expired_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cache_store_expired_at ON cache_store_unlogged (expired_at);
```

- `cache_key`: `cacheName::key` 형태
- `value`: Jackson으로 직렬화된 JSONB (타입 정보 포함)
- `expired_at`: 만료 시각
- unlogged: WAL 미사용으로 빠름, 크래시 시 손실 허용 (캐시)

## 컴포넌트

```
global/cache/
├── config/PgCacheConfig.kt   -- CacheManager 빈 등록, TTL 설정
├── PgCacheManager.kt         -- CacheManager 구현
└── PgCache.kt                -- Cache 구현 (JdbcTemplate CRUD)
```

## PgCache 동작

| 메서드 | SQL |
|--------|-----|
| `get(key)` | `SELECT value FROM ... WHERE cache_key = ? AND expired_at > now()` |
| `put(key, value)` | `INSERT ... ON CONFLICT DO UPDATE SET value = ?, expired_at = ?` |
| `evict(key)` | `DELETE FROM ... WHERE cache_key = ?` |
| `clear()` | `DELETE FROM ... WHERE cache_key LIKE 'cacheName::%'` |

## 설정

```yaml
custom:
  cache:
    ttl-seconds: 3600
```

## Eviction

- SELECT 시 `expired_at > now()` 필터링
- pg_cron 5분 주기로 만료 캐시 삭제: `DELETE FROM cache_store_unlogged WHERE expired_at <= now()`

## 직렬화

Jackson `ObjectMapper`에 `DefaultTyping` 활성화하여 JSONB에 타입 정보 포함. 역직렬화 시 원래 타입으로 복원.
