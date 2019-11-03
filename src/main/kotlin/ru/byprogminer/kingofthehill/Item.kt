package ru.byprogminer.kingofthehill

interface Item {

    fun use(player: User, currentState: State.Stage): State.Stage
}