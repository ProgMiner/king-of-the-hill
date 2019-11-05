package ru.byprogminer.kingofthehill

/**
 * Base event class
 */
sealed class Event {

    /**
     * Base player-fired event class
     */
    sealed class Player: Event() {

        abstract val player: User

        // Waiting
        data class Join(override val player: User): Player()
        data class Leave(override val player: User): Player()

        // Selecting
        data class SelectField(override val player: User, val fieldId: Int): Player()

        // Stages
        data class Step(override val player: User, val direction: Direction): Player()
        data class UseItem(override val player: User, val slot: Int): Player()
        data class GetItemList(override val player: User): Player()
    }

    /**
     * Base controller-fired event class
     */
    sealed class Controller: Event() {

        object NextState: Controller()
    }

    /**
     * Base game-fired event class
     */
    sealed class Game: Event() {

        abstract val game: AbstractGame

        data class PlayerJoined(override val game: AbstractGame, val player: User): Game()
        data class PlayerJoinedAlready(override val game: AbstractGame, val player: User): Game()

        data class PlayerLeft(override val game: AbstractGame, val player: User): Game()
        data class PlayerLeftAlready(override val game: AbstractGame, val player: User): Game()

        data class SelectingState(override val game: AbstractGame, val fields: List<Field>): Game()

        data class PlayerSelectedField(override val game: AbstractGame, val player: User, val fieldId: Int): Game()
        data class PlayerSelectedBusyField(override val game: AbstractGame, val player: User, val fieldId: Int): Game()
        data class AnotherPlayerSelectedField(override val game: AbstractGame, val player: User): Game()

        // TODO data class FirstStageState(override val game: AbstractGame): Game()

        data class PlayerStepped(override val game: AbstractGame, val player: User, val fieldId: Int): Game()
        data class PlayerStayedStand(override val game: AbstractGame, val player: User): Game()

        data class PlayerGotItemList(override val game: AbstractGame, val player: User, val items: List<Item>): Game()
        data class PlayerUsedItem(override val game: AbstractGame, val player: User, val item: Item): Game()
    }
}
