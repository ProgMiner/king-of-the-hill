package ru.byprogminer.kingofthehill

sealed class State(val state: Enum) {

    enum class Enum {

        WAITING, SELECTING, FIRST_STAGE, SECOND_STAGE, END
    }

    data class Waiting(val players: MutableSet<User> = mutableSetOf()): State(Enum.WAITING)

    data class Selecting(val players: MutableMap<User, Int?>, val fields: List<Field>): State(Enum.SELECTING)

    sealed class Stage(state: Enum): State(state) {

        abstract val players: MutableMap<User, Player>
        abstract val fields: List<Field>
        abstract val previousStage: Stage?

        data class First(
            override val players: MutableMap<User, Player>,
            override val fields: List<Field>,
            override val previousStage: Stage?
        ): Stage(Enum.FIRST_STAGE)

        data class Second(
            override val players: MutableMap<User, Player>,
            override val fields: List<Field>,
            override val previousStage: First
        ): Stage(Enum.SECOND_STAGE)
    }

    data class End(val lastStage: Stage): State(Enum.END)
}
