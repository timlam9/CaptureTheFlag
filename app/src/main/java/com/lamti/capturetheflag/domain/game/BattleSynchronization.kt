package com.lamti.capturetheflag.domain.game

import com.lamti.capturetheflag.utils.EMPTY

/** Pure transitions for use inside a game transaction. Null rejects the operation. */
object BattleSynchronization {

    fun add(game: Game, battle: Battle): Game? {
        if (game.gameState.state != ProgressState.Started || battle.battleID.isBlank() ||
            battle.state != BattleState.StandBy || battle.winner.isNotEmpty() ||
            battle.winnerID.isNotEmpty() || battle.players.size != 2 ||
            battle.players.any { it.id.isBlank() || it.ready }
        ) return null

        val participants = battle.players.map { it.id }.toSet()
        if (participants.size != 2) return null
        val red = game.redPlayers.filterNot { it.hasLost }.map { it.id }.toSet()
        val green = game.greenPlayers.filterNot { it.hasLost }.map { it.id }.toSet()
        if (participants.any { (it in red) == (it in green) } ||
            participants.count { it in red } != 1
        ) return null

        // A reciprocal challenge may have a different ID and reversed player order.
        if (game.battles.any {
                it.state != BattleState.Over && it.players.map { player -> player.id }.toSet() == participants
            }
        ) return game
        if (game.battles.any {
                it.battleID == battle.battleID || it.players.any { player -> player.id in participants }
            }
        ) return null
        return game.copy(battles = game.battles + battle)
    }

    fun ready(game: Game, battleID: String, playerID: String): Game? {
        val battle = game.battles.firstOrNull { it.battleID == battleID } ?: return null
        if (battle.players.none { it.id == playerID }) return null
        return when (battle.state) {
            BattleState.Over -> null
            BattleState.Started -> game
            BattleState.StandBy -> {
                val players = battle.players.map { if (it.id == playerID) it.copy(ready = true) else it }
                val starts = players.size == 2 && players.map { it.id }.distinct().size == 2 &&
                    players.all { it.ready }
                game.replaceBattle(battle.copy(
                    players = players,
                    state = if (starts) BattleState.Started else BattleState.StandBy
                ))
            }
        }
    }

    fun finish(game: Game, battleID: String, playerID: String, winnerName: String): Game? {
        val battle = game.battles.firstOrNull { it.battleID == battleID } ?: return null
        if (battle.players.none { it.id == playerID }) return null
        if (battle.state == BattleState.Over) return game
        if (battle.state != BattleState.Started || winnerName.isBlank()) return null
        return game.replaceBattle(battle.copy(
            state = BattleState.Over,
            winner = winnerName,
            winnerID = playerID
        ))
    }

    fun acknowledge(game: Game, battleID: String, playerID: String, playerName: String): Game? {
        val battle = game.battles.firstOrNull { it.battleID == battleID } ?: return game
        // The caller may already have acknowledged while the opponent remains reserved.
        if (battle.players.none { it.id == playerID }) return game
        val outcome = if (game.battleMiniGame == BattleMiniGame.None) {
            when (battle.state) {
                BattleState.StandBy -> {
                    val opponent = battle.players.singleOrNull { it.id != playerID } ?: return null
                    battle.copy(state = BattleState.Over, winner = EMPTY, winnerID = opponent.id)
                }
                BattleState.Over -> battle
                BattleState.Started -> return null
            }
        } else {
            if (battle.state != BattleState.Over || battle.winner.isBlank()) return null
            battle
        }
        val lost = when {
            outcome.winnerID.isNotBlank() -> outcome.winnerID != playerID
            game.battleMiniGame == BattleMiniGame.None -> outcome.winner != playerID
            else -> outcome.winner != playerName // Compatibility with existing name-only outcomes.
        }
        val remaining = outcome.copy(players = outcome.players.filterNot { it.id == playerID })
        val battles = game.battles.mapNotNull {
            if (it.battleID != battleID) it else remaining.takeIf { updated -> updated.players.isNotEmpty() }
        }
        if (!lost) return game.copy(battles = battles)
        return game.copy(
            battles = battles,
            redPlayers = game.redPlayers.map { if (it.id == playerID) it.copy(hasLost = true) else it },
            greenPlayers = game.greenPlayers.map { if (it.id == playerID) it.copy(hasLost = true) else it },
            gameState = game.gameState.copy(
                redFlagCaptured = game.gameState.redFlagCaptured.takeUnless { it == playerID },
                greenFlagCaptured = game.gameState.greenFlagCaptured.takeUnless { it == playerID }
            )
        )
    }

    fun leave(game: Game, playerID: String): Game = game.copy(
        redPlayers = game.redPlayers.filterNot { it.id == playerID },
        greenPlayers = game.greenPlayers.filterNot { it.id == playerID },
        // Cancel affected battles, releasing the opponent without eliminating them.
        battles = game.battles.filterNot { battle -> battle.players.any { it.id == playerID } },
        gameState = game.gameState.copy(
            redFlagCaptured = game.gameState.redFlagCaptured.takeUnless { it == playerID },
            greenFlagCaptured = game.gameState.greenFlagCaptured.takeUnless { it == playerID }
        )
    )

    private fun Game.replaceBattle(battle: Battle): Game = copy(
        battles = battles.map { if (it.battleID == battle.battleID) battle else it }
    )
}
