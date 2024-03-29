package com.lamti.capturetheflag.presentation.ui.fragments.ar

import android.location.Location
import android.os.CountDownTimer
import androidx.compose.runtime.State
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
import com.lamti.capturetheflag.domain.GameEngine
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.arcore.helpers.CloudAnchorManager
import com.lamti.capturetheflag.presentation.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.presentation.arcore.rendering.BackgroundRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PointCloudRenderer
import com.lamti.capturetheflag.presentation.ui.startTimer
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.CLOUD_ANCHOR_LOGGER_TAG
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.LOGGER_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val GREEN_COLOR = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)
private val RED_COLOR = floatArrayOf(255.0f, 0.0f, 0.0f, 255.0f)
private const val ERROR_MESSAGE = "An error occurred. Please relaunch the app and try again."

@HiltViewModel
class ArViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val cloudAnchorRepository: CloudAnchorRepository
) : ViewModel() {

    private var _session: MutableStateFlow<Session?> = MutableStateFlow(null)
    val session: StateFlow<Session?> = _session

    private val _message = MutableStateFlow(EMPTY)
    val message: StateFlow<String> = _message.asStateFlow()

    private val _time = MutableStateFlow(EMPTY)
    val time: StateFlow<String> = _time.asStateFlow()

    private val _instructions = MutableStateFlow(EMPTY)
    val instructions: StateFlow<String> = _instructions.asStateFlow()

    val player: StateFlow<Player> = gameEngine.player
    val game: State<Game> = gameEngine.game

    private val _captureFlag = MutableStateFlow(false)
    val captureFlag: StateFlow<Boolean> = _captureFlag.asStateFlow()

    private val _showPlacerButtons = MutableStateFlow(false)
    val showPlacerButtons: StateFlow<Boolean> = _showPlacerButtons.asStateFlow()

    private val _scannerMode = MutableStateFlow(false)

    private val cloudAnchorManager = CloudAnchorManager()
    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val virtualObject: ObjectRenderer = ObjectRenderer()
    private val planeRenderer: PlaneRenderer = PlaneRenderer()
    private val pointCloudRenderer: PointCloudRenderer = PointCloudRenderer()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val anchorMatrix = FloatArray(16)
    private var flagColor = GREEN_COLOR
    private var currentAnchor: Anchor? = null

    private var isResolveObjectStarted = false
    private var timer: CountDownTimer? = null

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
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
                Timber.e("[$LOGGER_TAG] Exception creating session: $exception")
                return
            }
        }
    }

    fun resumeSession() {
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            _session.value?.resume()
            if (_scannerMode.value) {
                if (!isResolveObjectStarted) {
                    isResolveObjectStarted = true
                    onResolveObjects()
                }
                if (timer == null) {
                    timer = startTimer(
                        timeInMillis = 60 * 1000,
                        onTick = {
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(it)
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(it)
                            _time.update { "$minutes:$seconds" }
                            _time.replayCache
                        },
                        onFinish = {
                            _time.update { EMPTY }
                            _captureFlag.update { true }
                            _message.update { "You discovered your opponent's flag by time's up. \'Capture\' it and run to the safehouse to win the game" }
                        }
                    )
                }
            }
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

    fun prepareRenderingObjects(prepareObjects: (BackgroundRenderer, PlaneRenderer, PointCloudRenderer, ObjectRenderer) -> Unit) {
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            prepareObjects(backgroundRenderer, planeRenderer, pointCloudRenderer, virtualObject)
        } catch (e: IOException) {
            Timber.e("[$LOGGER_TAG] Failed to read an asset file: $e")
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
                _message.update { getStartingMessage() }
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                _session.value?.getAllTrackables(Plane::class.java),
                camera.displayOrientedPose,
                projmtx
            )

            if (currentAnchor != null && currentAnchor?.trackingState == TrackingState.TRACKING) {
                flagColor = getFlagColor(player.value.gameDetails?.team)
                currentAnchor?.pose?.toMatrix(anchorMatrix, 0)
                // Update and draw the model and its shadow.
                virtualObject.updateModelMatrix(anchorMatrix, 1f)
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, flagColor)
            }
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Timber.e("[$LOGGER_TAG] Exception on the OpenGL thread: $t")
        }
    }

    fun createAnchor(anchor: Anchor) {
        currentAnchor = anchor
        sendAnchorToCloud()
    }

    fun isCurrentAnchorNull(): Boolean = currentAnchor != null

    fun setInstructions(text: String, isScanner: Boolean) {
        _instructions.update { text }
        _scannerMode.update { isScanner }
    }

    fun onCaptureClicked(onResult: (Boolean) -> Unit) = viewModelScope.launch { gameEngine.captureFlag(onResult) }

    fun onOkButtonPressed(onResult: (Boolean) -> Unit) = viewModelScope.launch { uploadFlagObject(onResult) }

    fun onCancelButtonPressed(text: String = "Tap to a discovered area to place your flag") {
        cloudAnchorManager.clearListeners()
        currentAnchor = null
        _showPlacerButtons.value = false
        _message.update { text }
    }

    /**
     * Checks if we detected at least one plane.
     */
    private fun hasTrackingPlane(): Boolean {
        if (_session.value == null) return false
        for (plane in _session.value!!.getAllTrackables(Plane::class.java)) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    private fun getFlagColor(team: Team?) = when (_scannerMode.value) {
        true -> if (team == Team.Red) GREEN_COLOR else RED_COLOR
        false -> if (team == Team.Red) RED_COLOR else GREEN_COLOR
    }

    private fun getStartingMessage() = when (_scannerMode.value) {
        true -> "Move your camera around to find your opponent's flag (or wait until the counter hit zero)"
        false -> "Tap to a discovered area to place your flag"
    }

    // Cloud anchors functions
    private suspend fun uploadFlagObject(onResult: (Boolean) -> Unit) {
        val newGame = when (player.value.gameDetails?.team) {
            Team.Red -> {
                val redFlag = game.value.gameState.redFlag.copy(isPlaced = true)
                val newGameState: GameState = game.value.gameState.copy(redFlag = redFlag)
                game.value.copy(gameState = newGameState)
            }
            Team.Green -> {
                val greenFlag = game.value.gameState.greenFlag.copy(isPlaced = true)
                val newGameState: GameState = game.value.gameState.copy(greenFlag = greenFlag)
                game.value.copy(gameState = newGameState)
            }
            else -> return
        }
        val latestGame = newGame.copy(
            gameState = if (newGame.gameState.redFlag.isPlaced && newGame.gameState.greenFlag.isPlaced) {
                newGame.gameState.copy(state = ProgressState.Started)
            } else {
                newGame.gameState
            }
        )
        onResult(cloudAnchorRepository.uploadGeofenceObject(latestGame))
    }

    private fun sendAnchorToCloud() {
        _message.update { "Your flag is placing. Please wait..." }
        cloudAnchorManager.hostCloudAnchor(_session.value, currentAnchor) { anchor: Anchor? ->
            Timber.d("[$CLOUD_ANCHOR_LOGGER_TAG] Uploading anchor to cloud: $anchor")
            onHostedAnchorAvailable(anchor!!)
        }
    }

    @Synchronized
    private fun onHostedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            val cloudAnchorId = anchor.cloudAnchorId

            viewModelScope.launch {
                val currentPosition: Location = gameEngine.getLastLocation()
                val newGame = when (player.value.gameDetails?.team) {
                    Team.Red -> {
                        val redFlag = game.value.gameState.redFlag.copy(
                            id = cloudAnchorId,
                            position = currentPosition.toLatLng(),
                            isPlaced = false,
                            isDiscovered = false,
                            timestamp = Date(),
                        )
                        val newGameState: GameState = game.value.gameState.copy(redFlag = redFlag)
                        game.value.copy(gameState = newGameState)
                    }
                    Team.Green -> {
                        val greenFlag = game.value.gameState.greenFlag.copy(
                            id = cloudAnchorId,
                            position = currentPosition.toLatLng(),
                            isPlaced = false,
                            isDiscovered = false,
                            timestamp = Date(),
                        )
                        val newGameState: GameState = game.value.gameState.copy(greenFlag = greenFlag)
                        game.value.copy(gameState = newGameState)
                    }
                    Team.Unknown -> return@launch
                    null -> return@launch
                }

                cloudAnchorRepository.uploadGeofenceObject(newGame)
                _message.update { "Your flag was placed successfully!" }
                _showPlacerButtons.update { true }
                currentAnchor = anchor
                Timber.d("[$CLOUD_ANCHOR_LOGGER_TAG] Anchor uploaded successfully: $anchor")
            }
        } else {
            _showPlacerButtons.update { false }
            onCancelButtonPressed(ERROR_MESSAGE)
            Timber.d("[$CLOUD_ANCHOR_LOGGER_TAG] Anchor failed to be uploaded")
        }
    }

    @Synchronized
    private fun onResolveObjects() {
        Timber.d("[$CLOUD_ANCHOR_LOGGER_TAG] On resolve objects")

        val redFlagID = game.value.gameState.redFlag.id
        val greenFlagID = game.value.gameState.greenFlag.id

        if (redFlagID.isEmpty() || greenFlagID.isEmpty()) {
            _message.update { "Flag was not found." }
            return
        }

        if (player.value.gameDetails?.team == Team.Red) {
            cloudAnchorManager.resolveCloudAnchor(_session.value, greenFlagID) { anchor ->
                onResolvedAnchorAvailable(anchor)
            }
        } else if (player.value.gameDetails?.team == Team.Green) {
            cloudAnchorManager.resolveCloudAnchor(_session.value, redFlagID) { anchor ->
                onResolvedAnchorAvailable(anchor)
            }
        }
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor) {
        Timber.d("[$CLOUD_ANCHOR_LOGGER_TAG] On cloud anchor available: $anchor")
        val cloudState = anchor.cloudAnchorState
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            _captureFlag.update { true }
            _message.update { "You discovered your opponent's flag. \'Capture\' it and run to the safehouse to win the game" }
            currentAnchor = anchor
        } else {
            _message.update { ERROR_MESSAGE }
        }
    }

}
