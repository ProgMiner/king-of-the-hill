package ru.byprogminer.kingofthehill.event

import java.lang.reflect.Parameter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class AbstractEventBus(
    private val threadPool: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Executors.defaultThreadFactory().newThread(r).apply { isDaemon = true }
    }
): EventBus {

    /**
     * <code>MutableMap&lt;Event type &rarr; MutableMap&lt;Listener &rarr; MutableList&lt;Handler wrapper&gt;&gt;&gt;</code>
     */
    private val handlers = mutableMapOf<Class<*>, MutableMap<Any, MutableList<(Any) -> Unit>>>()

    override fun attachListener(listener: Any) {
        threadPool.submit {
            for (method in listener::class.java.declaredMethods) {
                if (!method.isAnnotationPresent(EventHandler::class.java)) {
                    continue
                }

                var eventType: Class<*>? = null
                val parameters = mutableListOf<(Any, Any) -> Any?>()
                for (parameter in method.parameters) {
                    if (parameter.isAnnotationPresent(Event::class.java)) {
                        if (eventType == null) {
                            eventType = parameter.type
                        } else {
                            throw IllegalArgumentException()
                        }

                        parameters.add { _, e -> e }
                        continue
                    }

                    parameters.add(bindParameter(parameter))
                }

                handlers.computeIfAbsent(requireNotNull(eventType)) { mutableMapOf() }
                    .computeIfAbsent(listener) { mutableListOf() }.add { ev ->
                        method.invoke(listener, *Array(parameters.size) { i -> parameters[i](listener, ev) })
                    }
            }
        }
    }

    override fun detachListener(listener: Any) {
        threadPool.submit {
            for ((_, handlers) in handlers) {
                handlers.remove(listener)
            }
        }
    }

    override fun fireEvent(event: Any) {
        threadPool.submit {
            for ((eventClass, handlers) in handlers) {
                if (eventClass.isInstance(event)) {
                    for ((_, handlers1) in handlers) {
                        for (handler in handlers1) {
                            threadPool.submit { handler(event) }
                        }
                    }
                }
            }
        }
    }

    protected open fun bindParameter(parameter: Parameter): (Any, Any) -> Any? = when {
        EventBus::class.java.isAssignableFrom(parameter.type) -> { _, _ -> this }

        else -> throw IllegalArgumentException()
    }
}
