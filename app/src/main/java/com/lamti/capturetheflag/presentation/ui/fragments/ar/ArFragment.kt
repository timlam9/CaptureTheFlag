package com.lamti.capturetheflag.presentation.ui.fragments.ar

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentArBinding
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.arcore.helpers.CameraPermissionHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.DisplayRotationHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.TapHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.presentation.arcore.rendering.PlaneRenderer
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.components.composables.ar.ArComponents
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.utils.get
import com.lamti.capturetheflag.utils.myAppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@AndroidEntryPoint
class ArFragment : Fragment(R.layout.fragment_ar), GLSurfaceView.Renderer {

    private var binding: FragmentArBinding? = null
    private val viewModel: ArViewModel by viewModels()

    private var displayRotationHelper: DisplayRotationHelper? = null
    private var trackingStateHelper: TrackingStateHelper? = null
    private var tapHelper: TapHelper? = null

    private var installRequested = false
    private var arMode = ArMode.Scanner

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentArBinding.bind(view)

        setArMode()
        setupUI()
        setupHelpers()
        setupSurfaceView()
    }

    override fun onResume() {
        super.onResume()

        when (ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested)) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                return
            }
            ArCoreApk.InstallStatus.INSTALLED -> Unit
            else -> Unit
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            CameraPermissionHelper.requestCameraPermission(requireActivity())
            return
        }

        viewModel.createSession(Session(requireContext()))

        viewModel.resumeSession()
        displayRotationHelper!!.onResume()
        binding?.surfaceView?.onResume()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()

        viewModel.pauseSession()
        binding?.surfaceView?.onPause()
        displayRotationHelper!!.onPause()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, results: IntArray) {
        requestCameraPermission()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        viewModel.prepareRenderingObjects { backgroundRenderer, planeRenderer, pointCloudRenderer, virtualObject ->
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(requireContext())
            planeRenderer.createOnGlThread(requireContext(), "models/tri_grid.png")
            pointCloudRenderer.createOnGlThread(requireContext())

            virtualObject.createOnGlThread(requireContext(), "models/flag.obj", "models/flag.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (viewModel.session.value == null) return


        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper!!.updateSessionIfNeeded(viewModel.session.value)

        viewModel.renderObjects({ camera ->
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper!!.updateKeepScreenOnFlag(camera.trackingState)
        }) { frame, camera ->
            if (arMode == ArMode.Placer) handleTap(frame, camera)
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        if (viewModel.isCurrentAnchorNull()) return  // Do nothing if there was already an anchor.

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
                    viewModel.createAnchor(hit.createAnchor())
                    break
                }
            }
        }
    }

    private fun setArMode() {
        val mode = requireActivity().myAppPreferences.get(AR_MODE_KEY, ArMode.Scanner.name)

        arMode = when (mode) {
            ArMode.Placer.name -> {
                viewModel.setInstructions(getString(R.string.place_flag), false)
                ArMode.Placer
            }
            else -> {
                viewModel.setInstructions(getString(R.string.search_flag), true)
                ArMode.Scanner
            }
        }
    }

    private fun setupUI() = binding?.run {
        composeView.setContent {
            val instructions by viewModel.instructions.collectAsState()
            val message by viewModel.message.collectAsState()
            val showPlacerButtons by viewModel.showPlacerButtons.collectAsState()
            val showCaptureButton by viewModel.captureFlag.collectAsState()
            val player by viewModel.player.collectAsState()

            val arModeState by remember { mutableStateOf(arMode) }
            val teamColor = remember(player.gameDetails?.team) {
                when (player.gameDetails?.team) {
                    Team.Red -> Red
                    Team.Green -> Green
                    Team.Unknown -> Blue
                    null -> Blue
                }
            }

            ArComponents(
                instructions = instructions,
                message = message,
                arModeState = arModeState,
                showPlacerButtons = showPlacerButtons,
                showCaptureButton = showCaptureButton,
                okText = getString(R.string.place_flag),
                cancelText = getString(R.string.cancel),
                captureText = getString(R.string.capture_the_flag),
                teamColor = teamColor,
                onCancelClicked = { viewModel.onCancelButtonPressed() },
                onOkClicked = {
                    viewModel.onOkButtonPressed {
                        if (it) (requireActivity() as MainActivity).onBackPressed()
                    }
                }
            ) {
                viewModel.onCaptureClicked {
                    if (it) (requireActivity() as MainActivity).onBackPressed()
                }
            }
        }
    }

    private fun setupHelpers() {
        tapHelper = TapHelper(requireContext())
        trackingStateHelper = TrackingStateHelper(requireActivity())
        displayRotationHelper = DisplayRotationHelper(requireContext())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSurfaceView() = binding?.apply {
        surfaceView.setOnTouchListener(tapHelper)

        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        surfaceView.setRenderer(this@ArFragment)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setWillNotDraw(false)
    }

    private fun requestCameraPermission() {
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            Toast.makeText(
                requireContext(), "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(requireActivity())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(requireActivity())
            }
            requireActivity().finish()
        }
    }

}
