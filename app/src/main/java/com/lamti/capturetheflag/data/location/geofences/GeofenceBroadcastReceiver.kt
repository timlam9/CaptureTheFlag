package com.lamti.capturetheflag.data.location.geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val GEOFENCE_BROADCAST_RECEIVER_FILTER = "geofence_broadcast_receiver_filter"
const val GEOFENCE_KEY = "geofence_key"

abstract class HiltBroadcastReceiver : BroadcastReceiver() {

    @CallSuper
    override fun onReceive(context: Context?, intent: Intent?) {
    }
}

@AndroidEntryPoint
open class GeofenceBroadcastReceiver : HiltBroadcastReceiver() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject lateinit var firestoreRepository: FirestoreRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        handleIntent(intent ?: return, context ?: return)
    }

    private fun handleIntent(intent: Intent, context: Context) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("Error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val geofenceID = geofencingEvent.triggeringGeofences[0].requestId
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences)

            when {
                geofenceID.contains(GREEN) -> discoverFlag(Flag.Green)
                geofenceID.contains(RED) -> discoverFlag(Flag.Red)
                geofenceID.contains(SAFEHOUSE) -> checkGameOver()
            }

            notifyUserForGeofencesEvents(
                geofenceTransition = geofenceTransition,
                geofenceID = geofenceID,
                geofenceTransitionDetails = geofenceTransitionDetails,
                context = context
            )
        }
    }

    private fun checkGameOver() {
        applicationScope.launch {
            val player = firestoreRepository.getPlayer() ?: return@launch
            val gameDetails = player.gameDetails ?: return@launch
            val playerTeam = gameDetails.team
            val gameID = gameDetails.gameID
            val game = firestoreRepository.getGame(gameID) ?: return@launch

            if (player.userID == game.gameState.greenFlagCaptured) {
                if (playerTeam == Team.Red) {
                    firestoreRepository.endGame(Team.Red)
                }
            }
            if (player.userID == game.gameState.redFlagCaptured) {
                if (playerTeam == Team.Green) {
                    firestoreRepository.endGame(Team.Green)
                }
            }
        }
    }

    private fun notifyUserForGeofencesEvents(
        geofenceTransition: Int,
        geofenceID: String,
        geofenceTransitionDetails: String,
        context: Context
    ) {
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> sendEnterGeofenceIntent(geofenceID, context)
            Geofence.GEOFENCE_TRANSITION_EXIT -> sendExitGeofenceIntent(context)
            Geofence.GEOFENCE_TRANSITION_DWELL -> Timber.d(geofenceTransitionDetails)
            else -> Timber.e("Invalid type: $geofenceTransition")
        }
    }

    private fun sendEnterGeofenceIntent(geofenceID: String, context: Context) {
        Intent(GEOFENCE_BROADCAST_RECEIVER_FILTER).run {
            putExtra(GEOFENCE_KEY, geofenceID)
            context.sendBroadcast(this)
        }
    }

    private fun sendExitGeofenceIntent(context: Context) {
        Intent(GEOFENCE_BROADCAST_RECEIVER_FILTER).run {
            putExtra(GEOFENCE_KEY, EMPTY)
            context.sendBroadcast(this)
        }
    }

    private fun discoverFlag(flag: Flag) {
        applicationScope.launch {
            firestoreRepository.discoverFlag(flag)
        }
    }

    private fun getGeofenceTransitionDetails(
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val action = when (geofenceTransition) {
            1 -> "Enter"
            2 -> "Exit"
            4 -> "Dwell"
            else -> "other"
        }
        return "$action \"${triggeringGeofences[0].requestId}\" geofence"
    }

    companion object {

        private const val GREEN = "Green"
        private const val RED = "Red"
        private const val SAFEHOUSE = "Safehouse"
    }

}
