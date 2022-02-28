package com.lamti.capturetheflag.domain

import com.lamti.capturetheflag.domain.player.Player

interface FirestoreRepository {

    suspend fun getPlayer(id: String): Player

    suspend fun addPlayer(player: Player): Boolean

}
