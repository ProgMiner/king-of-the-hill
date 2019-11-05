package ru.byprogminer.kingofthehill

import ru.byprogminer.kingofthehill.event.EventBus
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AbstractGame {

    private var currentState: State = State.Waiting()
    private val stateLock = ReentrantReadWriteLock()

    protected open fun nextState() { // TODO
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

    protected fun onPlayer(event: Event.Player, eventBus: EventBus) {
        val currentState = stateLock.read { currentState }

        when (event) {
            is Event.Player.Join -> {
                if (currentState is State.Waiting) {
                    joinPlayer(currentState, event.player, eventBus)
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.Leave -> {
                if (currentState is State.Waiting) {
                    leavePlayer(currentState, event.player, eventBus)
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.SelectField -> {
                if (currentState is State.Selecting) {
                    selectStartingField(currentState, event.player, event.fieldId, eventBus)
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.Step -> {
                if (currentState is State.Stage) {
                    step(currentState, event.player, event.direction, eventBus)
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.GetItemList -> {
                if (currentState is State.Stage) {
                    getItemList(currentState, event.player, eventBus)
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.UseItem -> {
                if (currentState is State.Stage) {
                    useItem(currentState, event.player, currentState.players[event.player]!!.items[event.slot], eventBus)
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }

    protected open fun joinPlayer(currentState: State.Waiting, player: User, eventBus: EventBus) {
        if (currentState.players.add(player)) {
            eventBus.fireEvent(Event.Game.PlayerJoined(this, player))
        } else {
            eventBus.fireEvent(Event.Game.PlayerJoinedAlready(this, player))
        }
    }

    protected open fun leavePlayer(currentState: State.Waiting, player: User, eventBus: EventBus) {
        if (currentState.players.remove(player)) {
            eventBus.fireEvent(Event.Game.PlayerLeft(this, player))
        } else {
            eventBus.fireEvent(Event.Game.PlayerLeftAlready(this, player))
        }
    }

    protected open fun selectStartingField(currentState: State.Selecting, player: User, fieldId: Int, eventBus: EventBus) {
        if (!currentState.players.containsValue(fieldId)) {
            currentState.players[player] = fieldId

            eventBus.fireEvent(Event.Game.PlayerSelectedField(this, player, fieldId))
        } else {
            for ((p, f) in currentState.players) {
                if (f == fieldId) {
                    currentState.players[p] = null

                    eventBus.fireEvent(Event.Game.AnotherPlayerSelectedField(this, p))
                    break
                }
            }

            eventBus.fireEvent(Event.Game.PlayerSelectedBusyField(this, player, fieldId))
        }
    }

    protected open fun step(currentState: State.Stage, player: User, direction: Direction, eventBus: EventBus) {
        if (direction === Direction.STAND) {
            when {
                // TODO
            }
        }
    }

    protected open fun getItemList(currentState: State.Stage, player: User, eventBus: EventBus) {
        eventBus.fireEvent(Event.Game.PlayerGotItemList(this, player, currentState.players[player]!!.items))
    }

    protected open fun useItem(currentState: State.Stage, player: User, item: Item, eventBus: EventBus) {
        this.stateLock.write { this.currentState = item.use(player, currentState, eventBus) }

        eventBus.fireEvent(Event.Game.PlayerUsedItem(this, player, item))
    }

    protected abstract fun generateFields(): List<Field>
    protected abstract fun placeThings(busiedFields: Set<Int>, fields: List<Field>)
    protected abstract fun checkEnd(currentState: State.Stage): Boolean
    protected abstract fun getFieldByDirection(fields: List<Field>, fieldId: Int, direction: Direction): Int

    protected open fun afterState(currentState: State) {}
    protected open fun beforeState(currentState: State) {}
}
