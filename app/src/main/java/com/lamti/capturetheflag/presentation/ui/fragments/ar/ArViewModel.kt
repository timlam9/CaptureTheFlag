package com.lamti.capturetheflag.presentation.ui.fragments.ar

import android.location.Location
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.Game.Companion.initialGame
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Player.Companion.emptyPlayer
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.arcore.helpers.CloudAnchorManager
import com.lamti.capturetheflag.presentation.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.presentation.arcore.rendering.BackgroundRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PointCloudRenderer
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.EMPTY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date
import javax.inject.Inject

val TAG: String = ArFragment::class.java.simpleName
private val GREEN_COLOR = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)
private val RED_COLOR = floatArrayOf(255.0f, 0.0f, 0.0f, 255.0f)

@HiltViewModel
class ArViewModel @Inject constructor(
    private val cloudAnchorRepository: CloudAnchorRepository,
    private val locationRepository: LocationRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private var _session: MutableStateFlow<Session?> = MutableStateFlow(null)
    val session: StateFlow<Session?> = _session

    private val _message = MutableStateFlow(EMPTY)
    val message: StateFlow<String> = _message.asStateFlow()

    private val _instructions = MutableStateFlow(EMPTY)
    val instructions: StateFlow<String> = _instructions.asStateFlow()

    private val _player = mutableStateOf(emptyPlayer())
    val player: State<Player> = _player

    private val _game = MutableStateFlow(initialGame())
    val game: StateFlow<Game> = _game.asStateFlow()

    private val _showGrabButton = MutableStateFlow(false)
    val showGrabButton: StateFlow<Boolean> = _showGrabButton.asStateFlow()

    private val _showPlacerButtons = MutableStateFlow(false)
    val showPlacerButtons: StateFlow<Boolean> = _showPlacerButtons.asStateFlow()

    private val _scannerMode = MutableStateFlow(false)

    private val cloudAnchorManager = CloudAnchorManager()
    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val virtualObject: ObjectRenderer = ObjectRenderer()
    private val virtualObjectShadow: ObjectRenderer = ObjectRenderer()
    private val planeRenderer: PlaneRenderer = PlaneRenderer()
    private val pointCloudRenderer: PointCloudRenderer = PointCloudRenderer()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val anchorMatrix = FloatArray(16)
    private var flagColor = GREEN_COLOR
    private var currentAnchor: Anchor? = null

    init {
        viewModelScope.launch {
            _player.value = firestoreRepository.getPlayer() ?: emptyPlayer()
            flagColor = getFlagColor(_player.value.gameDetails?.team)
        }
        firestoreRepository.observeGame().onEach {
            _game.value = it
            if (_scannerMode.value) onResolveObjects()
        }.launchIn(viewModelScope)
    }

    private fun getFlagColor(team: Team?) = when (_scannerMode.value) {
        true -> if (team == Team.Red) GREEN_COLOR else RED_COLOR
        false -> if (team == Team.Red) RED_COLOR else GREEN_COLOR
    }

    fun createSession(session: Session?) {
        if (_session.value == null) {
            var exception: Exception? = null
            try {
                _session.update { session ?: return }

                // Configure session for cloud anchors.
                val config = Config(_session.value)
                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                _session.value?.configure(config)

            } catch (e: UnavailableArcoreNotInstalledException) {
                _message.update { "Please install ARCore" }
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                _message.update { "Please install ARCore" }
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                _message.update { "Please update ARCore" }
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                _message.update { "Please update this app" }
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                _message.update { "This device does not support AR" }
                exception = e
            } catch (e: Exception) {
                _message.update { "Failed to create AR session" }
                exception = e
            }
            if (_message.value != EMPTY) {
                Log.e(TAG, "Exception creating session", exception)
                return
            }
        }
    }

    fun resumeSession() {
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            _session.value?.resume()
        } catch (e: CameraNotAvailableException) {
            _message.update { "Camera not available. Try restarting the app." }
            _session.update { null }
            return
        }
    }

    fun pauseSession() {
        if (_session.value != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            _session.value?.pause()
        }
    }

    fun renderObjects(updateTrackingState: (camera: Camera) -> Unit, handleTap: (frame: Frame, camera: Camera) -> Unit) {
        try {
            _session.value?.setCameraTextureName(backgroundRenderer.textureId)

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera frame rate.
            val frame = _session.value?.update() ?: return
            cloudAnchorManager.onUpdate()

            val camera = frame.camera

            // Handle one tap per frame.
            handleTap(frame, camera)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame)

            updateTrackingState(camera)

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.trackingState == TrackingState.PAUSED) {
                _message.update { TrackingStateHelper.getTrackingFailureReasonString(camera) }
                return
            }

            // Get projection matrix.
            val projmtx = FloatArray(16)
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            val viewmtx = FloatArray(16)
            camera.getViewMatrix(viewmtx, 0)

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)
            frame.acquirePointCloud().use { pointCloud ->
                pointCloudRenderer.update(pointCloud)
                pointCloudRenderer.draw(viewmtx, projmtx)
            }

            // No tracking error at this point. If we didn't detect any plane, show searchingPlane message.
            if (!hasTrackingPlane()) {
                _message.update { "Searching for surfaces..." }
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                _session.value?.getAllTrackables(Plane::class.java),
                camera.displayOrientedPose,
                projmtx
            )

            if (currentAnchor != null && currentAnchor?.trackingState == TrackingState.TRACKING) {
                currentAnchor?.pose?.toMatrix(anchorMatrix, 0)
                // Update and draw the model and its shadow.
                virtualObject.updateModelMatrix(anchorMatrix, 1f)
                virtualObjectShadow.updateModelMatrix(anchorMatrix, 1f)
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, flagColor)
                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, flagColor)
            }
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    fun prepareRenderingObjects(prepareObjects: (BackgroundRenderer, PlaneRenderer, PointCloudRenderer, ObjectRenderer, ObjectRenderer) -> Unit) {
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            prepareObjects(backgroundRenderer, planeRenderer, pointCloudRenderer, virtualObject, virtualObjectShadow)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    @Synchronized
    fun onCancelButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()
        currentAnchor = null
    }

    @Synchronized
    private fun onResolveObjects() {
        viewModelScope.launch {
            val redFlagID = _game.value.gameState.redFlag.id
            val greenFlagID = _game.value.gameState.greenFlag.id

            if (redFlagID.isEmpty() || greenFlagID.isEmpty()) {
                _message.update { "A Cloud Anchor ID was not found." }
                return@launch
            }

            if (_player.value.gameDetails?.team == Team.Red) {
                cloudAnchorManager.resolveCloudAnchor(_session.value, greenFlagID) { anchor ->
                    onResolvedAnchorAvailable(anchor)
                }
            } else if (_player.value.gameDetails?.team == Team.Green) {
                Log.d("TAGARA", "Red flag: $redFlagID")
                cloudAnchorManager.resolveCloudAnchor(_session.value, redFlagID) { anchor ->
                    onResolvedAnchorAvailable(anchor)
                }
            }
        }
    }

    fun createAnchor(anchor: Anchor) {
        currentAnchor = anchor
        sendAnchorToCloud()
    }

    fun isCurrentAnchorNull(): Boolean = currentAnchor != null


    /**
     * Checks if we detected at least one plane.
     */
    private fun hasTrackingPlane(): Boolean {
        for (plane in _session.value!!.getAllTrackables(Plane::class.java)) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    @Synchronized
    private fun onHostedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            val cloudAnchorId = anchor.cloudAnchorId

            viewModelScope.launch {
                val currentPosition: Location = locationRepository.awaitLastLocation()
                val newGame = when (_player.value.gameDetails?.team) {
                    Team.Red -> {
                        val redFlag = _game.value.gameState.redFlag.copy(
                            id = cloudAnchorId,
                            position = currentPosition.toLatLng(),
                            isPlaced = false,
                            isDiscovered = false,
                            timestamp = Date(),
                        )
                        val newGameState: GameState = _game.value.gameState.copy(redFlag = redFlag)
                        _game.value.copy(gameState = newGameState)
                    }
                    Team.Green -> {
                        val greenFlag = _game.value.gameState.greenFlag.copy(
                            id = cloudAnchorId,
                            position = currentPosition.toLatLng(),
                            isPlaced = false,
                            isDiscovered = false,
                            timestamp = Date(),
                        )
                        val newGameState: GameState = _game.value.gameState.copy(greenFlag = greenFlag)
                        _game.value.copy(gameState = newGameState)
                    }
                    Team.Unknown -> return@launch
                    null -> return@launch
                }

                cloudAnchorRepository.uploadGeofenceObject(newGame)
            }
            _message.update { "Cloud Anchor Hosted. ID: $cloudAnchorId" }
            _showPlacerButtons.update { true }
            currentAnchor = anchor
        } else {
            _message.update { "Error while hosting: $cloudState" }
            _showPlacerButtons.update { false }
            onCancelButtonPressed()
        }
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            _showGrabButton.update { true }
            _instructions.update { "You discovered your opponent's flag. \'Grab\' it and go to safehouse to win the game" }
            currentAnchor = anchor
        } else {
            _message.update { "Error while resolving anchor with id: ${anchor.cloudAnchorId}. Error: $cloudState" }
        }
    }

    private fun sendAnchorToCloud() {
        // host cloud anchor
        _message.update { "Now hosting anchor..." }
        cloudAnchorManager.hostCloudAnchor(_session.value, currentAnchor) { anchor: Anchor? ->
            onHostedAnchorAvailable(anchor!!)
        }
    }

    fun setInstructions(text: String, scanner: Boolean) {
        _instructions.update { text }
        _scannerMode.update { scanner }
    }

    fun onOkButtonPressed(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val newGame = when (_player.value.gameDetails?.team) {
                Team.Red -> {
                    val redFlag = _game.value.gameState.redFlag.copy(isPlaced = true)
                    val newGameState: GameState = _game.value.gameState.copy(redFlag = redFlag)
                    _game.value.copy(gameState = newGameState)
                }
                Team.Green -> {
                    val greenFlag = _game.value.gameState.greenFlag.copy(isPlaced = true)
                    val newGameState: GameState = _game.value.gameState.copy(greenFlag = greenFlag)
                    _game.value.copy(gameState = newGameState)
                }
                Team.Unknown -> return@launch
                null -> return@launch
            }

            val latestGameState = if (newGame.gameState.redFlag.isPlaced && newGame.gameState.greenFlag.isPlaced) {
                newGame.gameState.copy(state = ProgressState.Started)
            } else {
                newGame.gameState
            }
            val latestGame = newGame.copy(gameState = latestGameState)

            val result = cloudAnchorRepository.uploadGeofenceObject(latestGame)
            onResult(result)
        }
    }

    fun onGrabPressed(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = firestoreRepository.grabTheFlag()
            onResult(result)
        }
    }

}
