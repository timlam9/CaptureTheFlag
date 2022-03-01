package com.lamti.capturetheflag.data

import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.utils.EMPTY

data class GameRaw(
    val gameID: String = EMPTY,
    val title: String = EMPTY,
    val gameState: GameStateRaw = GameStateRaw()
) {

    fun toGame() = Game(
        gameID = gameID,
        title = title,
        gameState = GameState(
            isGreenFlagDiscovered = gameState.greenFlagDiscovered,
            isRedFlagDiscovered = gameState.redFlagDiscovered,
            state = gameState.state.toState()
        )
    )
}

private fun String.toState(): ProgressState = when (this) {
    "Waiting" -> ProgressState.Waiting
    "Started" -> ProgressState.Started
    else -> ProgressState.Waiting
}


data class GameStateRaw(
    val greenFlagDiscovered: Boolean = false,
    val redFlagDiscovered: Boolean = false,
    val state: String = "Waiting"
)
