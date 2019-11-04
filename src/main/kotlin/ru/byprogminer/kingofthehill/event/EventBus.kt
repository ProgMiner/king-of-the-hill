package ru.byprogminer.kingofthehill.event

interface EventBus {

    fun attachListener(listener: Any)
    fun detachListener(listener: Any)

    fun fireEvent(event: Any)
}
