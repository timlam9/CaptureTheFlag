package com.lamti.capturetheflag.presentation.fragments.ar

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentArBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.CameraPermissionHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.DisplayRotationHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.SnackbarHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.TapHelper
import com.lamti.capturetheflag.presentation.arcore.helpers.TrackingStateHelper
import com.lamti.capturetheflag.presentation.arcore.rendering.ObjectRenderer
import com.lamti.capturetheflag.presentation.arcore.rendering.PlaneRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@AndroidEntryPoint
class ArFragment : Fragment(R.layout.fragment_ar), GLSurfaceView.Renderer {

    private var binding: FragmentArBinding? = null
    private val viewModel: ArViewModel by viewModels()

    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var trackingStateHelper: TrackingStateHelper? = null
    private var tapHelper: TapHelper? = null

    private var installRequested = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentArBinding.bind(view)

        setupHelpers()
        setupSurfaceView()
        setupListeners()
        collectFlows()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
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

    private fun setupListeners() = binding?.apply {
        resolveButton.setOnClickListener {
            viewModel.onResolveButtonPressed()
        }
        clearButton.setOnClickListener {
            viewModel.onClearButtonPressed()
        }
    }

    private fun collectFlows() {
        viewModel.message.onEach(::showSnackbarMessage).launchIn(lifecycleScope)
        viewModel.resolveButtonEnabled.onEach(::setResolveButtonActive).launchIn(lifecycleScope)
        viewModel.clearButtonEnabled.onEach(::setClearButtonActive).launchIn(lifecycleScope)
    }

    private fun showSnackbarMessage(message: String) {
        messageSnackbarHelper.showMessage(requireActivity(), message)
    }

    private fun setResolveButtonActive(active: Boolean) {
        binding?.resolveButton?.isEnabled = active
    }

    private fun setClearButtonActive(active: Boolean) {
        binding?.clearButton?.isEnabled = active
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


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        viewModel.prepareRenderingObjects { backgroundRenderer, planeRenderer, pointCloudRenderer, virtualObject, virtualObjectShadow ->
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(requireContext())
            planeRenderer.createOnGlThread(requireContext(), "models/tri_grid.png")
            pointCloudRenderer.createOnGlThread(requireContext())

            virtualObject.createOnGlThread(requireContext(), "models/andy.obj", "models/andy.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)

            virtualObjectShadow.createOnGlThread(requireContext(), "models/andy_shadow.obj", "models/andy_shadow.png")
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
        if (viewModel.session.value == null) return


        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper!!.updateSessionIfNeeded(viewModel.session.value)

        viewModel.renderObjects({ camera ->
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper!!.updateKeepScreenOnFlag(camera.trackingState)
        }) { frame, camera ->
            handleTap(frame, camera)
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

}
