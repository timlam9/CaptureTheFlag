package com.lamti.capturetheflag.domain.player

import com.lamti.capturetheflag.utils.EMPTY

data class Player(
    val userID: String,
    val team: Team,
    val details: PlayerDetails
) {

    companion object {

        fun emptyPlayer() = Player(
            userID = EMPTY,
            team = Team.Unknown,
            details = PlayerDetails(fullName = EMPTY, username = EMPTY, email = EMPTY)
        )
    }

}

