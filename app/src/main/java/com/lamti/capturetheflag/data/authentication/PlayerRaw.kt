package com.lamti.capturetheflag.data.authentication

import com.google.firebase.firestore.IgnoreExtraProperties
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY

@IgnoreExtraProperties
data class PlayerRaw(
    val userID: String = EMPTY,
    val status: String = "Online",
    val details: PlayerDetailsRaw = PlayerDetailsRaw(),
    val gameDetails: GameDetailsRaw? = null
) {

    fun toPlayer() = Player(
        userID = userID,
        status = status.toStatus(),
        details = details.toPlayerDetails(),
        gameDetails = gameDetails?.toGameDetails()
    )

    companion object {

        fun Player.toRaw() = PlayerRaw(
            userID = userID,
            status = status.name,
            details = PlayerDetailsRaw(
                fullName = details.fullName,
                username = details.username,
                email = details.email
            ),
            gameDetails = GameDetailsRaw(
                gameID = gameDetails?.gameID ?: EMPTY,
                team = gameDetails?.team?.name ?: EMPTY,
                rank = gameDetails?.rank?.name ?: EMPTY
            )
        )
    }
}

data class PlayerDetailsRaw(
    val fullName: String = EMPTY,
    val username: String = EMPTY,
    val email: String = EMPTY
) {

    fun toPlayerDetails() = PlayerDetails(
        fullName = fullName,
        username = username,
        email = email
    )
}

data class GameDetailsRaw(
    val gameID: String = EMPTY,
    val team: String = EMPTY,
    val rank: String = EMPTY
) {

    fun toGameDetails() = GameDetails(
        gameID = gameID,
        team = team.toTeam(),
        rank = rank.toRank()
    )
}

private fun String.toStatus() = when (this) {
    Player.Status.Online.name -> Player.Status.Online
    Player.Status.Playing.name -> Player.Status.Playing
    else -> Player.Status.Online
}

private fun String.toTeam() = when (this) {
    Team.Red.name -> Team.Red
    Team.Green.name -> Team.Green
    else -> Team.Unknown
}

private fun String.toRank() = when (this) {
    GameDetails.Rank.Soldier.name -> GameDetails.Rank.Soldier
    GameDetails.Rank.Leader.name -> GameDetails.Rank.Leader
    else -> GameDetails.Rank.Captain
}


