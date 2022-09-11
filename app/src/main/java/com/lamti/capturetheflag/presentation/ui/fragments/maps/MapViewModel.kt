package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.GameEngine
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.navigation.Screen
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(private val gameEngine: GameEngine) : ViewModel() {

    val livePosition: StateFlow<LatLng> = gameEngine.livePosition
    val initialPosition: StateFlow<LatLng> = gameEngine.initialPosition

    val game: State<Game> = gameEngine.game
    val player: StateFlow<Player> = gameEngine.player
    val otherPlayers: StateFlow<List<GamePlayer>> = gameEngine.otherPlayers

    val initialScreen: StateFlow<Screen> = gameEngine.initialScreen
    val stayInSplashScreen: StateFlow<Boolean> = gameEngine.stayInSplashScreen
    val enterBattleScreen: StateFlow<Boolean> = gameEngine.enterBattleScreen
    val enterGameOverScreen: StateFlow<Boolean> = gameEngine.enterGameOverScreen

    val showArFlagButton: StateFlow<Boolean> = gameEngine.showArFlagButton
    val showBattleButton: StateFlow<String> = gameEngine.showBattleButton
    val isSafehouseDraggable: StateFlow<Boolean> = gameEngine.isSafehouseDraggable
    val canPlaceFlag: StateFlow<Boolean> = gameEngine.canPlaceFlag

    val qrCodeBitmap: StateFlow<Bitmap?> = gameEngine.qrCodeBitmap

    val arMode: StateFlow<ArMode> = gameEngine.arMode

    init {
        getLastLocation()
        observePlayer()
        observeGame()
        gameEngine.startLocationUpdates()
    }

    private fun getLastLocation() = viewModelScope.launch(Dispatchers.IO) { gameEngine.getLastLocation() }

    private fun observePlayer() = gameEngine.observePlayer()
    private fun observeGame() = gameEngine.observeGame()

    suspend fun getGame(id: String): Game? = gameEngine.getGame(id)

    fun setEnteredGeofenceId(id: String) = gameEngine.setEnteredGeofenceId(id)

    fun logout() = gameEngine.logout()

    fun onCreateGameClicked(title: String) = viewModelScope.launch { gameEngine.createNewGameWithRedCaptain(title) }

    fun onGameCodeScanned(gameID: String) = viewModelScope.launch { gameEngine.addPlayerToGame(gameID) }

    fun onTeamButtonClicked(team: Team) = gameEngine.changePlayerTeam(team)

    fun onTeamOkButtonClicked() = viewModelScope.launch { gameEngine.addPlayerToTeam() }

    fun onStartGameClicked() = viewModelScope.launch { gameEngine.startGame() }

    fun onReadyButtonClicked(position: LatLng, gameRadius: Float, flagRadius: Float) = viewModelScope.launch {
        gameEngine.updateSafehouseAndForwardGameState(position, gameRadius, flagRadius)
    }

    fun onBattleButtonClicked() = viewModelScope.launch { gameEngine.createBattle() }

    fun onLostBattleButtonClicked() = viewModelScope.launch { gameEngine.looseBattle() }

    fun onGameOverOkClicked(onResult: (Boolean) -> Unit) = viewModelScope.launch(Dispatchers.Main) {
        gameEngine.removePlayer(onResult)
    }

    fun onArCorelessCaptured(onResult: (Boolean) -> Unit) = viewModelScope.launch {
        //TODO: Add Timer!
        gameEngine.captureFlag(onResult)
    }
}
