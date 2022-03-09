package com.lamti.capturetheflag.presentation.ui.components.navigation

import com.lamti.capturetheflag.utils.EMPTY

sealed class Screen(open val route: String = EMPTY) {

    object Map : Screen("map")

    object Menu : Screen("menu")

    object CreateGame : Screen("create_game")

    object JoinGame : Screen("join_game")

    object ConnectingToGame : Screen("connecting_to_game")

}
