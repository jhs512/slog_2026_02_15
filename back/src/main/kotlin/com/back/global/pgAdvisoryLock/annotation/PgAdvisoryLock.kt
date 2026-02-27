package com.back.global.pgAdvisoryLock.annotation

/**
 * PostgreSQL 어드바이저리 락을 메서드 단위로 적용합니다.
 *
 * [key]는 SpEL 표현식으로, 메서드 파라미터를 #paramName 형식으로 참조할 수 있습니다.
 *
 * 사용 예시:
 * ```kotlin
 * @PgAdvisoryLock(key = "#postId")
 * fun deletePost(postId: Int) { ... }
 *
 * @PgAdvisoryLock(key = "'comment:' + #postId")
 * fun addComment(postId: Int) { ... }
 * ```
 *
 * 락을 획득하지 못하면 [com.back.global.pgAdvisoryLock.app.PgAdvisoryLockAcquisitionException]을 던집니다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgAdvisoryLock(val key: String)
