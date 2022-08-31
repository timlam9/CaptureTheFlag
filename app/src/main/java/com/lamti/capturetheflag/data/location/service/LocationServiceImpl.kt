package com.lamti.capturetheflag.data.location.service

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.Game.Companion.initialGame
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_BATTLE_RANGE
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.SERVICE_LOCATION_LOGGER_TAG
import com.lamti.capturetheflag.utils.emptyPosition
import com.lamti.capturetheflag.utils.isInRangeOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationServiceImpl @Inject constructor() : LifecycleService() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var firestoreRepository: FirestoreRepository

    private var isServiceRunning = false
    private var locationUpdates: Job? = null
    private var observePlayerJob: Job? = null

    private val _livePosition: MutableStateFlow<LatLng> = MutableStateFlow(emptyPosition())
    private val _game: MutableStateFlow<Game> = MutableStateFlow(initialGame())
    private val _player: MutableStateFlow<Player> = MutableStateFlow(Player.emptyPlayer())
    private val _showBattleNotification: MutableStateFlow<String> = MutableStateFlow(EMPTY)

    private val battleSound: Uri = Uri.parse("android.resource://com.lamti.capturetheflag/" + R.raw.battle_found)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] ${getSerializable(SERVICE_COMMAND)} command is received")

            when (getSerializable(SERVICE_COMMAND) as LocationServiceCommand) {
                LocationServiceCommand.Start -> {
                    startFlagForegroundService()
                    startLocationUpdates()
                    observePlayer()
                    showNotificationListener()
                }
                LocationServiceCommand.Stop -> {
                    isServiceRunning = false
                    locationUpdates?.cancel()
                    observePlayerJob?.cancel()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        return START_STICKY
    }

    private fun observePlayer() {
        observePlayerJob = lifecycleScope.launch {
            firestoreRepository.observePlayer().onEach { player ->
                _player.update { player }
                player.run {
                    if (gameDetails?.gameID?.isNotEmpty() == true) {
                        _game.update { firestoreRepository.getGame(gameDetails.gameID) ?: initialGame() }
                        observeOtherPlayers(
                            playerID = userID,
                            gameID = gameDetails.gameID,
                            playerTeam = gameDetails.team
                        )
                    }
                }
            }.flowOn(Dispatchers.IO)
                .launchIn(lifecycleScope)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] Location Service is created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] Location Service is destroyed")

        isServiceRunning = false
        locationUpdates?.cancel()
    }

    private fun startFlagForegroundService() {
        if (!isServiceRunning) startForeground(NotificationHelper.NOTIFICATION_ID, notificationHelper.getNotification())
        isServiceRunning = true
    }

    private fun startLocationUpdates() {
        locationUpdates = locationRepository.locationFlow()
            .onEach { location ->
                _livePosition.update { location.toLatLng() }
                firestoreRepository.uploadGamePlayer(location.toLatLng())
            }
            .flowOn(Dispatchers.IO)
            .launchIn(lifecycleScope)
    }

    private fun observeOtherPlayers(playerID: String, gameID: String, playerTeam: Team) =
        lifecycleScope.launch {
            firestoreRepository.observePlayersPosition(gameID)
                .onEach { players -> foundOpponentToBattle(players, playerID, playerTeam) }
                .flowOn(Dispatchers.IO)
                .launchIn(lifecycleScope)
        }

    private fun foundOpponentToBattle(
        players: List<GamePlayer>,
        playerID: String,
        playerTeam: Team
    ) {
        var foundOpponent = false
        for (player in players) {
            if (!isAppInForegrounded() &&
                !_game.value.battles.flatMap { it.playersIDs }.contains(player.id) &&
                player.team != playerTeam &&
                player.id != playerID &&
                player.position.isInBattleableGameZone() &&
                _livePosition.value.isInBattleableGameZone() &&
                _livePosition.value.isInRangeOf(player.position, DEFAULT_BATTLE_RANGE)
            ) {
                _showBattleNotification.value = "${player.username}"
                foundOpponent = true
                break
            }
        }
        if (!foundOpponent) {
            _showBattleNotification.value = EMPTY
        }
    }

    private fun showNotificationListener() = _showBattleNotification.onEach {
        if (it != EMPTY) notificationHelper.showEventNotification(
            title = "Opponent Found: $it",
            content = "Tap to battle him",
            sound = battleSound
        )
    }.flowOn(Dispatchers.IO)
        .launchIn(lifecycleScope)

    // Position extensions
    private fun LatLng.isInBattleableGameZone() =
        isInsideGame() && !isInsideSafehouse() && !isInsideRedFlag() && !isInsideGreenFlag()

    private fun LatLng.isInsideGame() = isInRangeOf(_game.value.gameState.safehouse.position, _game.value.gameRadius)

    private fun LatLng.isInsideSafehouse() = isInRangeOf(_game.value.gameState.safehouse.position, DEFAULT_SAFEHOUSE_RADIUS)

    private fun LatLng.isInsideGreenFlag() = isInRangeOf(_game.value.gameState.greenFlag.position, DEFAULT_FLAG_RADIUS)

    private fun LatLng.isInsideRedFlag() = isInRangeOf(_game.value.gameState.redFlag.position, DEFAULT_FLAG_RADIUS)

    companion object {

        const val SERVICE_COMMAND = "service_command"
    }

}
