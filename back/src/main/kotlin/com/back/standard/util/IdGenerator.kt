package com.back.standard.util

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.util.*

@Component
class IdGenerator(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val idPool = ThreadLocal<MutableMap<String, ArrayDeque<Long>>>()

    fun genId(name: String, count: Int = 1): Long {
        val pools = idPool.get()
        val pool = pools?.get(name)
        val poolSize = pool?.size ?: 0

        if (poolSize < count) {
            val need = count - poolSize

            if (need == 1) {
                val id = jdbcTemplate.queryForObject<Long>("SELECT nextval('${name}_id_seq')")!!
                val target = pool
                    ?: ArrayDeque<Long>(count).also {
                        val p = pools ?: mutableMapOf<String, ArrayDeque<Long>>().also { m -> idPool.set(m) }
                        p[name] = it
                    }
                target.add(id)
            } else {
                val endVal = jdbcTemplate.queryForObject<Long>(
                    "SELECT setval('${name}_id_seq', nextval('${name}_id_seq') + ?)",
                    need - 1,
                )!!
                val startVal = endVal - (need - 1)

                val target = pool
                    ?: ArrayDeque<Long>(count).also {
                        val p = pools ?: mutableMapOf<String, ArrayDeque<Long>>().also { m -> idPool.set(m) }
                        p[name] = it
                    }
                for (id in startVal..endVal) {
                    target.add(id)
                }
            }
        }

        val result = idPool.get()[name]!!
        val id = result.poll()

        if (result.isEmpty()) {
            val p = idPool.get()
            p.remove(name)
            if (p.isEmpty()) idPool.remove()
        }

        return id
    }
}
