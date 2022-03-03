package com.lamti.capturetheflag.data.authentication

import com.google.firebase.firestore.IgnoreExtraProperties
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY

@IgnoreExtraProperties
data class PlayerRaw(
    val userID: String = EMPTY,
    val team: String = "Unknown",
    val details: PlayerDetailsRaw = PlayerDetailsRaw()
) {

    fun toPlayer() = Player(
        userID = userID,
        team = team.toTeam(),
        details = PlayerDetails(
            fullName = details.fullName,
            username = details.username,
            email = details.email
        )
    )

    companion object {

        fun Player.toRaw() = PlayerRaw(
            userID = userID,
            team = team.name,
            details = PlayerDetailsRaw(
                fullName = details.fullName,
                username = details.username,
                email = details.email
            )
        )
    }

}

private fun String.toTeam() = when (this) {
    Team.Red.name -> Team.Red
    Team.Green.name -> Team.Green
    else -> Team.Unknown
}

data class PlayerDetailsRaw(
    val fullName: String = EMPTY,
    val username: String = EMPTY,
    val email: String = EMPTY
)
