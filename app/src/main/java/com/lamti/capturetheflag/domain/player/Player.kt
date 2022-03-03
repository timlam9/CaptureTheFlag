package com.lamti.capturetheflag.domain.player

import com.lamti.capturetheflag.utils.EMPTY

data class Player(
    val userID: String,
    val status: Status,
    val details: PlayerDetails,
    val gameDetails: GameDetails?
) {

    companion object {

        fun emptyPlayer() = Player(
            userID = EMPTY,
            status = Status.Online,
            details = PlayerDetails(fullName = EMPTY, username = EMPTY, email = EMPTY),
            gameDetails = null
        )
    }

    enum class Status {
        Online,
        Playing
    }
}

data class GameDetails(
    val gameID: String,
    val team: Team,
    val rank: Rank
) {

    enum class Rank {
        Soldier,
        Leader,
        Captain
    }
}
