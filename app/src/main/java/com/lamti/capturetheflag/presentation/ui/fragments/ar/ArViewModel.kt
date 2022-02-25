package com.lamti.capturetheflag.presentation.ui.fragments.ar

import android.util.Log
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
import com.lamti.capturetheflag.data.CloudAnchor
import com.lamti.capturetheflag.domain.CloudAnchorRepository
import com.lamti.capturetheflag.presentation.arcore.helpers.CloudAnchorManager
import com.lamti.capturetheflag.presentation.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.presentation.arcore.rendering.BackgroundRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PointCloudRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

val TAG: String = ArFragment::class.java.simpleName

@HiltViewModel
class ArViewModel @Inject constructor(private val cloudAnchorRepository: CloudAnchorRepository) : ViewModel() {

    private var _session: MutableStateFlow<Session?> = MutableStateFlow(null)
    val session: StateFlow<Session?> = _session

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _resolveButtonEnabled = MutableStateFlow(true)
    val resolveButtonEnabled: StateFlow<Boolean> = _resolveButtonEnabled.asStateFlow()

    private val _clearButtonEnabled = MutableStateFlow(true)
    val clearButtonEnabled: StateFlow<Boolean> = _clearButtonEnabled.asStateFlow()

    private val cloudAnchorManager = CloudAnchorManager()

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val virtualObject: ObjectRenderer = ObjectRenderer()
    private val virtualObjectShadow: ObjectRenderer = ObjectRenderer()
    private val planeRenderer: PlaneRenderer = PlaneRenderer()
    private val pointCloudRenderer: PointCloudRenderer = PointCloudRenderer()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val anchorMatrix = FloatArray(16)
    private val andyColor = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)
    private var currentAnchor: Anchor? = null

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
            if (_message.value != "") {
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
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, andyColor)
                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, andyColor)
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
    fun onClearButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()
        _resolveButtonEnabled.update { true }
        currentAnchor = null
    }

    @Synchronized
    fun onResolveButtonPressed() {
        viewModelScope.launch {
            val cloudAnchor: CloudAnchor = cloudAnchorRepository.getUploadedAnchor()
            val anchorID = cloudAnchor.anchorID

            if (anchorID.isEmpty()) {
                _message.update { "A Cloud Anchor ID was not found." }
                return@launch
            }

            _resolveButtonEnabled.update { false }
            cloudAnchorManager.resolveCloudAnchor(_session.value, anchorID) { anchor ->
                onResolvedAnchorAvailable(anchor)
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

            viewModelScope.launch { cloudAnchorRepository.uploadAnchor(CloudAnchor(cloudAnchorId)) }
            _message.update { "Cloud Anchor Hosted. ID: $cloudAnchorId" }
            currentAnchor = anchor
        } else {
            _message.update { "Error while hosting: $cloudState" }
        }
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            _message.update { "Cloud Anchor Resolved. ID: ${anchor.cloudAnchorId}" }
            currentAnchor = anchor
        } else {
            _message.update { "Error while resolving anchor with id: ${anchor.cloudAnchorId}. Error: $cloudState" }
            _resolveButtonEnabled.update { true }
        }
    }

    private fun sendAnchorToCloud() {
        // host cloud anchor
        _resolveButtonEnabled.update { false }
        _message.update { "Now hosting anchor..." }
        cloudAnchorManager.hostCloudAnchor(_session.value, currentAnchor) { anchor: Anchor? ->
            onHostedAnchorAvailable(anchor!!)
        }
    }

}
