package com.back.global.task.`in`

import com.back.global.task.app.TaskHandlerRegistry
import com.back.global.task.out.TaskRepository
import com.back.standard.dto.TaskPayload
import com.back.standard.util.Ut
import org.slf4j.LoggerFactory
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executors

@Component
class TaskProcessingScheduledJob(
    private val taskRepository: TaskRepository,
    private val taskHandlerRegistry: TaskHandlerRegistry,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = LoggerFactory.getLogger(TaskProcessingScheduledJob::class.java)
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    @Scheduled(fixedDelayString = "\${custom.task.processor.fixedDelayMs}")
    @SchedulerLock(name = "processTasks", lockAtLeastFor = "PT1M")
    fun processTasks() {
        reclaimStaleTasks()

        val taskIds = transactionTemplate.execute {
            val pendingTasks = taskRepository.findPendingTasksWithLock(10)
            pendingTasks.forEach { it.markAsProcessing() }
            pendingTasks.map { it.id }
        }

        taskIds?.forEach { taskId ->
            executor.submit { executeTask(taskId) }
        }
    }

    /**
     * 처리 중 프로세스가 죽어 PROCESSING에 멈춘 task를 PENDING으로 되돌린다.
     * 되돌리지 않으면 조회 쿼리(PENDING만 봄)에 잡히지 않아 영구히 방치된다.
     */
    private fun reclaimStaleTasks() {
        val reclaimed = transactionTemplate.execute {
            taskRepository.reclaimStaleProcessingTasks(STALE_PROCESSING_MINUTES)
        } ?: 0

        if (reclaimed > 0) {
            logger.warn("Reclaimed $reclaimed stale PROCESSING task(s) older than $STALE_PROCESSING_MINUTES minutes")
        }
    }

    companion object {
        // 이 시간 넘게 PROCESSING에 머문 task는 처리가 끊긴 것으로 보고 회수한다.
        // 정상 처리는 수 초 안에 끝나므로 넉넉한 값이다.
        private const val STALE_PROCESSING_MINUTES = 10
    }

    private fun executeTask(taskId: Int) = transactionTemplate.execute {
        val task = taskRepository.findById(taskId).orElse(null) ?: return@execute

        try {
            val entry = taskHandlerRegistry.getEntry(task.taskType)

            if (entry != null) {
                val payload = Ut.JSON.fromString(task.payload, entry.payloadClass) as TaskPayload
                entry.handlerMethod.method.invoke(entry.handlerMethod.bean, payload)
                task.markAsCompleted()
            } else {
                logger.warn("No handler found for task type: ${task.taskType}")
                task.errorMessage = "No handler found"
                task.scheduleRetry()
            }
        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            logger.error("Task failed: $taskId (retry: ${task.retryCount}/${task.maxRetries})", rootCause)
            task.errorMessage = rootCause.message ?: rootCause::class.simpleName
            task.scheduleRetry()
        }
    }
}
