package com.lamti.capturetheflag.data.location.geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import javax.inject.Inject

const val GEOFENCE_BROADCAST_RECEIVER_FILTER = "geofence_broadcast_receiver_filter"
const val ENTER_GEOFENCE_KEY = "enter_geofence_key"

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
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val geofenceID = geofencingEvent.triggeringGeofences[0].requestId

            Log.d("TAGARA", "Geofence: $geofenceID")

            when {
                geofenceID.contains(GREEN) -> discoverFlag(Flag.Green)
                geofenceID.contains(RED) -> discoverFlag(Flag.Red)
                geofenceID.contains(SAFEHOUSE) -> {
                    applicationScope.launch {
                        Log.d("TAGARA", "Safehouse")
                        val player = firestoreRepository.getPlayer() ?: return@launch
                        Log.d("TAGARA", "Player: $player")
                        val game = firestoreRepository.getGame(player.gameDetails?.gameID ?: EMPTY) ?: return@launch

                        Log.d("TAGARA", "Game: $game")

                        if (player.userID == game.gameState.greenFlagGrabbed) {
                            if (player.gameDetails?.team == Team.Red) {
                                firestoreRepository.endGame(Team.Red)
                            }
                        }
                        if (player.userID == game.gameState.redFlagGrabbed) {
                            Log.d("TAGARA", "Red flag grabbed")
                            if (player.gameDetails?.team == Team.Green) {
                                Log.d("TAGARA", "from green team")
                                firestoreRepository.endGame(Team.Green)
                            }
                        }
                    }
                }
            }
        }

        notifyUserForGeofencesEvents(geofenceTransition, geofencingEvent, context)
    }

    private fun notifyUserForGeofencesEvents(
        geofenceTransition: Int,
        geofencingEvent: GeofencingEvent,
        context: Context
    ) {
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeringGeofences
            )

            // if enter a geofence
            if (geofenceTransition == 1) {
                val geofenceID = triggeringGeofences[0].requestId
                val intent = Intent(GEOFENCE_BROADCAST_RECEIVER_FILTER)
                intent.putExtra(ENTER_GEOFENCE_KEY, geofenceID)
                context.sendBroadcast(intent)
            }

            Toast.makeText(context, geofenceTransitionDetails, Toast.LENGTH_SHORT).show()
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            Log.e(TAG, "invalid type: $geofenceTransition")
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
            else -> "other"
        }
        return "$action \"${triggeringGeofences[0].requestId}\" geofence"
    }

    companion object {

        const val TAG = "TAGARA_GEOFENCE"
        private const val GREEN = "Green"
        private const val RED = "Red"
        private const val SAFEHOUSE = "Safehouse"
    }

}

//fun Context.registerReceiverInScope(
//    scope: CoroutineScope,
//    vararg intentFilterActions: String,
//    callback: (Intent) -> Unit,
//) {
//    val receiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            callback(intent)
//        }
//    }
//    val intentFilter = IntentFilter()
//    intentFilterActions.forEach { intentFilter.addAction(it) }
//    registerReceiver(receiver, intentFilter)
//    scope.coroutineContext.job.invokeOnCompletion {
//        unregisterReceiver(receiver)
//    }
//}
