package com.back.global.redisLock.annotation

/**
 * Redis 분산 락을 메서드 단위로 적용합니다.
 *
 * [key]는 SpEL 표현식으로, 메서드 파라미터를 #paramName 형식으로 참조할 수 있습니다.
 * Redisson Watchdog이 락을 자동 연장하므로 별도 타임아웃 설정이 불필요합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @RedisLock(key = "#postId")
 * fun deletePost(postId: Int) { ... }
 *
 * @RedisLock(key = "'comment:' + #postId")
 * fun addComment(postId: Int) { ... }
 * ```
 *
 * 락을 획득하지 못하면 [com.back.global.redisLock.app.RedisLockAcquisitionException]을 던집니다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisLock(val key: String)
