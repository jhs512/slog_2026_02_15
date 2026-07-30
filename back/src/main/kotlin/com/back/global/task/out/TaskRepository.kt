package com.back.global.task.out

import com.back.global.task.domain.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TaskRepository : JpaRepository<Task, Int> {
    @Query(
        value = """
            SELECT *
            FROM task
            WHERE status = 'PENDING'
            AND next_retry_at <= NOW()
            ORDER BY next_retry_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findPendingTasksWithLock(limit: Int = 10): List<Task>

    /**
     * PROCESSING으로 표시된 뒤 오래 진전이 없는 task를 회수한다.
     *
     * markAsProcessing 이후 프로세스가 죽거나 트랜잭션이 끊기면 그 task는 PROCESSING에 남는데,
     * 조회 쿼리가 PENDING만 보므로 아무도 다시 집지 않아 영구 미아가 된다
     * (운영에서 두 달 넘게 방치된 사례가 있었다).
     *
     * 처리 시작 시각을 따로 저장하지 않으므로 modified_at을 근사치로 쓴다. PROCESSING 상태에서는
     * 다른 수정이 일어나지 않아 사실상 처리 시작 시각과 같고, 어긋나더라도 회수가 조금 늦어질 뿐이다.
     */
    @Modifying
    @Query(
        value = """
            UPDATE task
            SET status = 'PENDING', next_retry_at = NOW()
            WHERE status = 'PROCESSING'
            AND modified_at < NOW() - MAKE_INTERVAL(mins => :staleMinutes)
        """,
        nativeQuery = true
    )
    fun reclaimStaleProcessingTasks(staleMinutes: Int): Int
}
