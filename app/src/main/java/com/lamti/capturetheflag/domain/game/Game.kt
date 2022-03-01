package com.lamti.capturetheflag.domain.game

data class Game(
    val gameID: String,
    val title: String,
    val gameState: GameState
)

data class GameState(
    val isGreenFlagDiscovered: Boolean,
    val isRedFlagDiscovered: Boolean,
    val state: ProgressState
)

enum class ProgressState {
    Waiting,
    Started
}
