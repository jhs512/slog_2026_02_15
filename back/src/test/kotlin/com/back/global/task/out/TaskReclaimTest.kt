package com.back.global.task.out

import com.back.global.task.domain.Task
import com.back.global.task.domain.TaskStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * PROCESSING에 멈춘 task 회수 검증.
 *
 * markAsProcessing 이후 프로세스가 죽으면 그 task는 PROCESSING에 남고, 조회 쿼리가 PENDING만
 * 보므로 아무도 다시 집지 않는다. 운영에서 두 달 넘게 방치된 사례가 있었다.
 * 회수는 modified_at을 근사치로 쓰므로, 오래된 것만 골라내는지가 핵심이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TaskReclaimTest {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private fun saveProcessingTask(): Int {
        val task = taskRepository.save(
            Task(
                uid = UUID.randomUUID(),
                aggregateType = "Test",
                aggregateId = 1,
                taskType = "test.reclaim",
                payload = "{}",
                status = TaskStatus.PROCESSING,
            )
        )
        entityManager.flush()
        return task.id
    }

    /** modified_at은 @LastModifiedDate라 앱에서 바꿀 수 없으므로 네이티브 쿼리로 과거로 민다 */
    private fun ageTask(id: Int, minutes: Int) {
        entityManager
            .createNativeQuery("UPDATE task SET modified_at = NOW() - MAKE_INTERVAL(mins => :m) WHERE id = :id")
            .setParameter("m", minutes)
            .setParameter("id", id)
            .executeUpdate()
    }

    private fun statusOf(id: Int): String =
        entityManager
            .createNativeQuery("SELECT status FROM task WHERE id = :id")
            .setParameter("id", id)
            .singleResult as String

    @Test
    fun `오래 멈춘 PROCESSING task는 PENDING으로 회수된다`() {
        val id = saveProcessingTask()
        ageTask(id, 30)

        val reclaimed = taskRepository.reclaimStaleProcessingTasks(10)

        assertThat(reclaimed).isGreaterThanOrEqualTo(1)
        assertThat(statusOf(id)).isEqualTo("PENDING")
    }

    @Test
    fun `방금 시작한 PROCESSING task는 회수되지 않는다`() {
        val id = saveProcessingTask()

        taskRepository.reclaimStaleProcessingTasks(10)

        assertThat(statusOf(id))
            .describedAs("처리 중인 task를 회수하면 같은 작업이 중복 실행된다")
            .isEqualTo("PROCESSING")
    }
}
