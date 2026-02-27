package com.back.global.pgAdvisoryLock.app

import com.back.global.pgAdvisoryLock.annotation.PgAdvisoryLock
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.sql.DataSource

@Aspect
@Component
class PgAdvisoryLockAspect(
    private val dataSource: DataSource,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(pgAdvisoryLock)")
    fun lock(pjp: ProceedingJoinPoint, pgAdvisoryLock: PgAdvisoryLock): Any? {
        val key = evaluateKey(pjp, pgAdvisoryLock.key)
        val lockId = keyToLong(key)

        // 트랜잭션 커넥션과 분리된 전용 커넥션 사용 (락 유지 범위 = 메서드 실행 시간)
        val conn = dataSource.connection
        try {
            val locked = conn.prepareStatement("SELECT pg_try_advisory_lock(?)").use { ps ->
                ps.setLong(1, lockId)
                ps.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
            }

            if (!locked) {
                conn.close()
                throw PgAdvisoryLockAcquisitionException(key)
            }
        } catch (e: PgAdvisoryLockAcquisitionException) {
            throw e
        } catch (e: Exception) {
            conn.close()
            throw e
        }

        log.debug("Advisory Lock 획득: key={}, lockId={}", key, lockId)

        return try {
            pjp.proceed()
        } finally {
            try {
                conn.prepareStatement("SELECT pg_advisory_unlock(?)").use { ps ->
                    ps.setLong(1, lockId)
                    ps.executeQuery()
                }
                log.debug("Advisory Lock 해제: key={}, lockId={}", key, lockId)
            } finally {
                conn.close()
            }
        }
    }

    private fun evaluateKey(pjp: ProceedingJoinPoint, keyExpression: String): String {
        val method = (pjp.signature as MethodSignature).method
        val paramNames = nameDiscoverer.getParameterNames(method) ?: emptyArray()

        val context = StandardEvaluationContext()
        paramNames.forEachIndexed { i, name -> context.setVariable(name, pjp.args[i]) }

        return parser.parseExpression(keyExpression).getValue(context, String::class.java)
            ?: throw IllegalArgumentException("Advisory Lock 키 표현식이 null로 평가됨: $keyExpression")
    }

    companion object {
        fun keyToLong(key: String): Long {
            val bytes = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            return ByteBuffer.wrap(bytes).long
        }
    }
}

class PgAdvisoryLockAcquisitionException(key: String) :
    RuntimeException("Advisory Lock 획득 실패: key=$key")
