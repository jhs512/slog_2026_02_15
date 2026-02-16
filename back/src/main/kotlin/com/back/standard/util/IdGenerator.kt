package com.back.standard.util

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class IdGenerator(
    private val em: EntityManager,
) {
    private val pools = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()
    private val locks = ConcurrentHashMap<String, Any>()
    private val defaultPoolSize = 100

    fun genId(name: String): Long {
        val pool = pools.computeIfAbsent(name) { ConcurrentLinkedQueue() }

        pool.poll()?.let { return it }

        // 풀이 빈 경우에만 synchronized — 100개 소진 시에만 발생
        val lock = locks.computeIfAbsent(name) { Any() }

        synchronized(lock) {
            // 대기 중 다른 쓰레드가 이미 충전했을 수 있음
            pool.poll()?.let { return it }

            @Suppress("UNCHECKED_CAST")
            val ids = em.createNativeQuery(
                "SELECT nextval('${name}_id_seq') FROM generate_series(1, :count)"
            )
                .setParameter("count", defaultPoolSize)
                .resultList as List<Long>
            pool.addAll(ids)
        }

        return pool.poll()!!
    }
}
