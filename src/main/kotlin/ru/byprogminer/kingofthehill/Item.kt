package ru.byprogminer.kingofthehill

import ru.byprogminer.kingofthehill.event.EventBus

interface Item {

    fun use(player: User, currentState: State.Stage, eventBus: EventBus): State.Stage
}
