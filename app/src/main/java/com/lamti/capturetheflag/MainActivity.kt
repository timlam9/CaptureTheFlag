package com.lamti.capturetheflag

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Config.CloudAnchorMode
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.lamti.capturetheflag.arcore.helpers.CameraPermissionHelper
import com.lamti.capturetheflag.arcore.helpers.CloudAnchorManager
import com.lamti.capturetheflag.arcore.helpers.DisplayRotationHelper
import com.lamti.capturetheflag.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.arcore.helpers.SnackbarHelper
import com.lamti.capturetheflag.arcore.helpers.TapHelper
import com.lamti.capturetheflag.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.arcore.rendering.BackgroundRenderer
import com.lamti.capturetheflag.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.arcore.rendering.PointCloudRenderer
import com.lamti.capturetheflag.data.FirebaseManager
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private val TAG: String = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var binding: ActivityMainBinding

    private val firebaseManager = FirebaseManager()
    private val cloudAnchorManager = CloudAnchorManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setup view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onInitialize()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()

        createSession()
        resumeSession()
    }

    override fun onPause() {
        super.onPause()

        pauseSession()
    }

    private fun createSession() {
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(this@MainActivity, !installRequested)) {
                    InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    InstallStatus.INSTALLED -> Unit
                    else -> Unit
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this@MainActivity)) {
                    CameraPermissionHelper.requestCameraPermission(this@MainActivity)
                    return
                }

                // Create the session.
                session = Session(this@MainActivity)

                // Configure session for cloud anchors.
                val config = Config(session)
                config.cloudAnchorMode = CloudAnchorMode.ENABLED
                session!!.configure(config)

            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }
            if (message != null) {
                messageSnackbarHelper.showError(this@MainActivity, message)
                Log.e(TAG, "Exception creating session", exception)
                return
            }
        }
    }

    private fun resumeSession() {
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(this@MainActivity, "Camera not available. Try restarting the app.")
            session = null
            return
        }
        binding.surfaceView.onResume()
        displayRotationHelper!!.onResume()
    }

    private fun pauseSession() {
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper!!.onPause()
            binding.surfaceView.onPause()
            session!!.pause()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, results: IntArray) {
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        if (!CameraPermissionHelper.hasCameraPermission(this@MainActivity)) {
            Toast.makeText(
                this@MainActivity, "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this@MainActivity)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this@MainActivity)
            }
            this@MainActivity.finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    private fun setupListeners() = with(binding) {
        resolveButton.setOnClickListener {
            onResolveButtonPressed()
        }
        clearButton.setOnClickListener {
            onClearButtonPressed()
        }
    }

    private var installRequested = false

    private var session: Session? = null
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var trackingStateHelper: TrackingStateHelper? = null
    private var tapHelper: TapHelper? = null

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val virtualObject: ObjectRenderer = ObjectRenderer()
    private val virtualObjectShadow: ObjectRenderer = ObjectRenderer()
    private val planeRenderer: PlaneRenderer = PlaneRenderer()
    private val pointCloudRenderer: PointCloudRenderer = PointCloudRenderer()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val anchorMatrix = FloatArray(16)
    private val andyColor = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)

    private var currentAnchor: Anchor? = null

    private fun onInitialize() {
        setupHelpers()
        setupSurfaceView()
    }

    private fun setupHelpers() {
        tapHelper = TapHelper(this@MainActivity)
        trackingStateHelper = TrackingStateHelper(this@MainActivity)
        displayRotationHelper = DisplayRotationHelper(this@MainActivity)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSurfaceView() = with(binding) {
        surfaceView.setOnTouchListener(tapHelper)

        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        surfaceView.setRenderer(this@MainActivity)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setWillNotDraw(false)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        prepareRenderingObjects()
    }

    private fun prepareRenderingObjects() {
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this@MainActivity)
            planeRenderer.createOnGlThread(this@MainActivity, "models/tri_grid.png")
            pointCloudRenderer.createOnGlThread(this@MainActivity)

            virtualObject.createOnGlThread(this@MainActivity, "models/andy.obj", "models/andy.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)

            virtualObjectShadow.createOnGlThread(this@MainActivity, "models/andy_shadow.obj", "models/andy_shadow.png")
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow)
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (session == null) return


        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper!!.updateSessionIfNeeded(session)

        renderObjects()
    }

    private fun renderObjects() {
        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera frame rate.
            val frame = session!!.update()
            cloudAnchorManager.onUpdate()

            val camera = frame.camera

            // Handle one tap per frame.
            handleTap(frame, camera)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame)

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper!!.updateKeepScreenOnFlag(camera.trackingState)

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.trackingState == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                    this@MainActivity, TrackingStateHelper.getTrackingFailureReasonString(camera)
                )
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
                messageSnackbarHelper.showMessage(this@MainActivity, "Searching for surfaces...")
            }

            // Visualize planes.
            planeRenderer.drawPlanes(session!!.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projmtx)

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

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        if (currentAnchor != null) return  // Do nothing if there was already an anchor.

        val tap = tapHelper!!.poll()
        if (tap != null && camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon
                val trackable = hit.trackable
                // Creates an anchor if a plane or an oriented point was hit.
                if ((trackable is Plane
                            && trackable.isPoseInPolygon(hit.hitPose)
                            && PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0)
                    || (trackable is Point
                            && trackable.orientationMode
                            == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                ) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    currentAnchor = hit.createAnchor()

                    // host cloud anchor
                    runOnUiThread { binding.resolveButton.isEnabled = false }
                    messageSnackbarHelper.showMessage(this@MainActivity, "Now hosting anchor...")
                    cloudAnchorManager.hostCloudAnchor(session, currentAnchor) { anchor: Anchor? ->
                        onHostedAnchorAvailable(anchor!!)
                    }
                    break
                }
            }
        }
    }

    /**
     * Checks if we detected at least one plane.
     */
    private fun hasTrackingPlane(): Boolean {
        for (plane in session!!.getAllTrackables(Plane::class.java)) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    @Synchronized
    private fun onClearButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()
        binding.resolveButton.isEnabled = true
        currentAnchor = null
    }

    @Synchronized
    private fun onHostedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == CloudAnchorState.SUCCESS) {
            val cloudAnchorId = anchor.cloudAnchorId

            firebaseManager.uploadAnchor(cloudAnchorId)
            messageSnackbarHelper.showMessage(this@MainActivity, "Cloud Anchor Hosted. ID: $cloudAnchorId")
            currentAnchor = anchor
        } else {
            messageSnackbarHelper.showMessage(this@MainActivity, "Error while hosting: $cloudState")
        }
    }

    @Synchronized
    private fun onResolveButtonPressed() {
        firebaseManager.getUploadedAnchorID { cloudAnchorId ->
            if (cloudAnchorId.isEmpty()) {
                messageSnackbarHelper.showMessage(this@MainActivity, "A Cloud Anchor ID was not found.")
                return@getUploadedAnchorID
            }
            binding.resolveButton.isEnabled = false
            cloudAnchorManager.resolveCloudAnchor(session, cloudAnchorId) { anchor ->
                onResolvedAnchorAvailable(anchor)
            }
        }
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor) {
        val cloudState = anchor.cloudAnchorState
        if (cloudState == CloudAnchorState.SUCCESS) {
            messageSnackbarHelper.showMessage(this@MainActivity, "Cloud Anchor Resolved. ID: ${anchor.cloudAnchorId}")
            currentAnchor = anchor
        } else {
            messageSnackbarHelper.showMessage(
                this@MainActivity,
                "Error while resolving anchor with id: ${anchor.cloudAnchorId}. Error: $cloudState"
            )
            binding.resolveButton.isEnabled = true
        }
    }

}
