package ru.byprogminer.kingofthehill

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AbstractGame {

    private var currentState: State = State.Waiting()
    private val stateLock = ReentrantReadWriteLock()

    protected open fun nextState() {
        stateLock.write {
            afterState(currentState)

            currentState = when (val currentState = currentState) {
                is State.Waiting -> State.Selecting(currentState.players
                    .map { it to null }.toMap().toMutableMap(),
                    generateFields())

                is State.Selecting -> {
                    placeThings(currentState.players.values.map { it!! }.toSet(), currentState.fields)

                    State.Stage.First(currentState.players.entries.map { (u, f) -> u to Player(u, f!!) }
                        .toMap().toMutableMap(), currentState.fields, null)
                }

                is State.Stage -> {
                    if (checkEnd(currentState)) {
                        State.End(currentState)
                    }

                    when (currentState) {
                        is State.Stage.First ->
                            State.Stage.Second(currentState.players.toMutableMap(), currentState.fields, currentState)
                        else ->
                            State.Stage.First(currentState.players.toMutableMap(), currentState.fields, currentState)
                    }
                }

                is State.End -> return
            }

            beforeState(currentState)
        }
    }

    open fun joinPlayer(player: User): Boolean {
        when (val currentState = stateLock.read { currentState }) {
            is State.Waiting -> return currentState.players.add(player)
            else -> throw IllegalStateException()
        }
    }

    open fun leavePlayer(player: User): Boolean {
        when (val currentState = stateLock.read { currentState }) {
            is State.Waiting -> return currentState.players.remove(player)
            else -> throw IllegalStateException()
        }
    }

    fun endWaiting() {
        when (stateLock.read { currentState }) {
            is State.Waiting -> nextState()

            else -> throw IllegalStateException()
        }
    }

    fun selectStartingField(player: User, field: Field): Boolean {
        when (val currentState = stateLock.read { currentState }) {
            is State.Selecting ->
                return if (!currentState.players.containsValue(field)) {
                    currentState.players[player] = field

                    true
                } else {
                    false
                }

            else -> throw IllegalStateException()
        }
    }

    fun endSelecting() {
        when (stateLock.read { currentState }) {
            is State.Selecting -> nextState()

            else -> throw IllegalStateException()
        }
    }

    open fun step(player: User, direction: Direction) {
        when (val currentState = stateLock.read { currentState }) {
            is State.Stage.First -> currentState.players[player]?.let { pl ->
                currentState.players[player] = pl.copy(field = getFieldByDirection(pl.field, direction))
            }

            else -> throw IllegalStateException()
        }
    }

    open fun useItem(player: User, item: Item) {
        when (val currentState = stateLock.read { currentState }) {
            is State.Stage -> stateLock.write { this.currentState = item.use(player, currentState) }
            else -> throw IllegalStateException()
        }
    }

    fun endFirstStage() {
        when (stateLock.read { currentState }) {
            is State.Stage.First -> nextState()

            else -> throw IllegalStateException()
        }
    }

    fun getCurrentState() = stateLock.read { currentState.state }

    protected abstract fun generateFields(): List<Field>
    protected abstract fun placeThings(busiedFields: Set<Field>, fields: List<Field>)
    protected abstract fun checkEnd(currentState: State.Stage): Boolean
    protected abstract fun getFieldByDirection(field: Field, direction: Direction): Field

    protected open fun afterState(currentState: State) {}
    protected open fun beforeState(currentState: State) {}
}
