package com.lamti.capturetheflag

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.lamti.capturetheflag.arcore.helpers.CameraPermissionHelper
import com.lamti.capturetheflag.arcore.helpers.DisplayRotationHelper
import com.lamti.capturetheflag.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.arcore.helpers.SnackbarHelper
import com.lamti.capturetheflag.arcore.helpers.TapHelper
import com.lamti.capturetheflag.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel = MainViewModel()

    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var trackingStateHelper: TrackingStateHelper? = null
    private var tapHelper: TapHelper? = null

    private var installRequested = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHelpers()
        setupSurfaceView()
        setupListeners()
        collectFlows()
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

    private fun setupListeners() = with(binding) {
        resolveButton.setOnClickListener {
            mainViewModel.onResolveButtonPressed()
        }
        clearButton.setOnClickListener {
            mainViewModel.onClearButtonPressed()
        }
    }

    private fun collectFlows() {
        mainViewModel.message.onEach(::showSnackbarMessage).launchIn(lifecycleScope)
        mainViewModel.resolveButtonEnabled.onEach(::setResolveButtonActive).launchIn(lifecycleScope)
        mainViewModel.clearButtonEnabled.onEach(::setClearButtonActive).launchIn(lifecycleScope)
    }

    private fun showSnackbarMessage(message: String) {
        messageSnackbarHelper.showMessage(this@MainActivity, message)
    }

    private fun setResolveButtonActive(active: Boolean) {
        binding.resolveButton.isEnabled = active
    }

    private fun setClearButtonActive(active: Boolean) {
        binding.clearButton.isEnabled = active
    }

    override fun onResume() {
        super.onResume()

        when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                return
            }
            InstallStatus.INSTALLED -> Unit
            else -> Unit
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        mainViewModel.createSession(Session(this@MainActivity))

        mainViewModel.resumeSession()
        displayRotationHelper!!.onResume()
        binding.surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()

        mainViewModel.pauseSession()
        binding.surfaceView.onPause()
        displayRotationHelper!!.onPause()
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


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        mainViewModel.prepareRenderingObjects { backgroundRenderer, planeRenderer, pointCloudRenderer, virtualObject, virtualObjectShadow ->
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this@MainActivity)
            planeRenderer.createOnGlThread(this@MainActivity, "models/tri_grid.png")
            pointCloudRenderer.createOnGlThread(this@MainActivity)

            virtualObject.createOnGlThread(this@MainActivity, "models/andy.obj", "models/andy.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)

            virtualObjectShadow.createOnGlThread(this@MainActivity, "models/andy_shadow.obj", "models/andy_shadow.png")
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow)
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (mainViewModel.session.value == null) return


        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper!!.updateSessionIfNeeded(mainViewModel.session.value)

        mainViewModel.renderObjects({ camera ->
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper!!.updateKeepScreenOnFlag(camera.trackingState)
        }) { frame, camera ->
            handleTap(frame, camera)
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        if (mainViewModel.isCurrentAnchorNull()) return  // Do nothing if there was already an anchor.

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
                    mainViewModel.createAnchor(hit.createAnchor())
                    break
                }
            }
        }
    }

}
