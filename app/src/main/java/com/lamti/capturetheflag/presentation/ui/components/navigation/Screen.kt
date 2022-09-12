package com.lamti.capturetheflag.presentation.ui.components.navigation

import com.lamti.capturetheflag.utils.EMPTY

sealed class Screen(open val route: String = EMPTY) {

    object Map : Screen("map")

    object Menu : Screen("menu")

    object CreateGame : Screen("create_game")

    object StartingGame : Screen("starting_game")

    object JoinGame : Screen("join_game")

    object ChooseTeam : Screen("choose_team")

    object Battle : Screen("battle")

    object BattleWon : Screen("battle_won")

    object GameOver : Screen("game_over")

}
