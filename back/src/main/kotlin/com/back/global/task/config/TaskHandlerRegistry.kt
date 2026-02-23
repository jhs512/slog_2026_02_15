package com.back.global.task.config

import com.back.global.task.domain.TaskHandler
import com.back.standard.dto.Task
import com.back.standard.dto.TaskPayload
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.lang.reflect.Method

data class TaskHandlerMethod(
    val bean: Any,
    val method: Method
)

data class TaskHandlerEntry(
    val payloadClass: Class<out TaskPayload>,
    val handlerMethod: TaskHandlerMethod,
)

@Component
class TaskHandlerRegistry(
    private val applicationContext: ApplicationContext
) : ApplicationListener<ContextRefreshedEvent> {

    private val byType = mutableMapOf<String, TaskHandlerEntry>()
    private val typeByClass = mutableMapOf<Class<out TaskPayload>, String>()

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        applicationContext.beanDefinitionNames.forEach { beanName ->
            val bean = applicationContext.getBean(beanName)

            bean::class.java.methods
                .filter { it.isAnnotationPresent(TaskHandler::class.java) }
                .forEach { method ->
                    val parameterTypes = method.parameterTypes

                    if (parameterTypes.size == 1 && TaskPayload::class.java.isAssignableFrom(parameterTypes[0])) {
                        @Suppress("UNCHECKED_CAST")
                        val payloadClass = parameterTypes[0] as Class<out TaskPayload>
                        val type = payloadClass.getAnnotation(Task::class.java)?.type
                            ?: error("No @Task annotation on ${payloadClass.simpleName}")
                        check(!byType.containsKey(type)) {
                            "Duplicate @TaskHandler for type '$type': " +
                                "already registered by ${byType[type]!!.handlerMethod.method.declaringClass.simpleName}, " +
                                "duplicate found in ${bean::class.java.simpleName}"
                        }
                        byType[type] = TaskHandlerEntry(payloadClass, TaskHandlerMethod(bean, method))
                        typeByClass[payloadClass] = type
                    }
                }
        }
    }

    fun getHandler(payloadClass: Class<out TaskPayload>): TaskHandlerMethod? {
        val type = typeByClass[payloadClass] ?: return null
        return byType[type]?.handlerMethod
    }

    fun getType(payloadClass: Class<out TaskPayload>): String? = typeByClass[payloadClass]

    fun getEntry(type: String): TaskHandlerEntry? = byType[type]
}
