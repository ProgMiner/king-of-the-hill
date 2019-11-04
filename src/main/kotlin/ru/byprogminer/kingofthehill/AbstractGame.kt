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
                    eventBus.fireEvent(joinPlayer(currentState, event.player))
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.Leave -> {
                if (currentState is State.Waiting) {
                    eventBus.fireEvent(leavePlayer(currentState, event.player))
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.SelectField -> {
                if (currentState is State.Selecting) {
                    eventBus.fireEvent(selectStartingField(currentState, event.player, currentState.fields[event.fieldId]))
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.Step -> {
                // TODO
            }

            is Event.Player.GetItemList -> {
                if (currentState is State.Stage) {
                    eventBus.fireEvent(getItemList(currentState, event.player))
                } else {
                    throw IllegalStateException()
                }
            }

            is Event.Player.UseItem -> {
                if (currentState is State.Stage) {
                    eventBus.fireEvent(useItem(currentState, event.player,
                        currentState.players[event.player]!!.items[event.slot], eventBus))
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }

    protected open fun joinPlayer(currentState: State.Waiting, player: User): Event.Game =
        if (currentState.players.add(player)) {
            Event.Game.PlayerJoined(this, player)
        } else {
            Event.Game.PlayerJoinedAlready(this, player)
        }

    protected open fun leavePlayer(currentState: State.Waiting, player: User): Event.Game =
        if (currentState.players.remove(player)) {
            Event.Game.PlayerLeft(this, player)
        } else {
            Event.Game.PlayerLeftAlready(this, player)
        }

    protected open fun selectStartingField(currentState: State.Selecting, player: User, field: Field): Event.Game =
        if (!currentState.players.containsValue(field)) {
            currentState.players[player] = field

            Event.Game.PlayerSelectedField(this, player, field)
        } else {
            Event.Game.PlayerSelectedBusyField(this, player, field)
        }

    protected open fun getItemList(currentState: State.Stage, player: User): Event.Game =
        Event.Game.PlayerGotItemList(this, player, currentState.players[player]!!.items)

    protected open fun useItem(currentState: State.Stage, player: User, item: Item, eventBus: EventBus): Event.Game {
        this.stateLock.write { this.currentState = item.use(player, currentState, eventBus) }

        return Event.Game.PlayerUsedItem(this, player, item)
    }

    protected abstract fun generateFields(): List<Field>
    protected abstract fun placeThings(busiedFields: Set<Field>, fields: List<Field>)
    protected abstract fun checkEnd(currentState: State.Stage): Boolean
    protected abstract fun getFieldByDirection(field: Field, direction: Direction): Field

    protected open fun afterState(currentState: State) {}
    protected open fun beforeState(currentState: State) {}
}
