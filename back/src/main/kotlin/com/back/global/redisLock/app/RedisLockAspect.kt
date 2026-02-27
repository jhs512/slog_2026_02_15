package com.back.global.redisLock.app

import com.back.global.redisLock.annotation.RedisLock
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Aspect
@Component
class RedisLockAspect(
    private val redissonClient: RedissonClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(redisLock)")
    fun lock(pjp: ProceedingJoinPoint, redisLock: RedisLock): Any? {
        val key = "redislock:" + evaluateKey(pjp, redisLock.key)
        val lock = redissonClient.getLock(key)

        // waitTime=0: 즉시 실패, leaseTime 미지정: Watchdog이 자동 연장
        if (!lock.tryLock(0, TimeUnit.SECONDS)) throw RedisLockAcquisitionException(key)

        log.debug("Redis Lock 획득: key={}", key)

        return try {
            pjp.proceed()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                log.debug("Redis Lock 해제: key={}", key)
            }
        }
    }

    private fun evaluateKey(pjp: ProceedingJoinPoint, keyExpression: String): String {
        val method = (pjp.signature as MethodSignature).method
        val paramNames = nameDiscoverer.getParameterNames(method) ?: emptyArray()

        val context = StandardEvaluationContext()
        paramNames.forEachIndexed { i, name -> context.setVariable(name, pjp.args[i]) }

        return parser.parseExpression(keyExpression).getValue(context, String::class.java)
            ?: throw IllegalArgumentException("Redis Lock 키 표현식이 null로 평가됨: $keyExpression")
    }
}

class RedisLockAcquisitionException(key: String) :
    RuntimeException("Redis Lock 획득 실패: key=$key")
