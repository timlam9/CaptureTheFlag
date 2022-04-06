package com.lamti.capturetheflag.presentation.ui.components.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.QrCodeAnalyzer
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.InfoTextField
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.White
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.LOGGER_TAG
import timber.log.Timber

@Composable
fun JoinGameScreen(onQrCodeScanned: (String) -> Unit) {
    val openDialog = remember { mutableStateOf(false) }

    QrCodeScanner(onCodeChanged = onQrCodeScanned)
    QrCodeContent(openDialog)
    CustomDialog(
        openDialog = openDialog.value,
        onDismissDialog = { openDialog.value = false },
        onJoinClicked = onQrCodeScanned
    )
}

@Composable
fun QrCodeScanner(onCodeChanged: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCamPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCamPermission = granted }
    )
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCamPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val previewView = PreviewView(context)
                    val preview = Preview.Builder().build()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setImageQueueDepth(10)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer { result ->
                            onCodeChanged(result)
                        }
                    )
                    try {
                        cameraProviderFuture.get().bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Timber.e("[$LOGGER_TAG] Scan code: ${e.message}")
                        e.printStackTrace()
                    }
                    previewView
                }
            )
        }
    }
}

@Composable
fun QrCodeContent(openDialog: MutableState<Boolean>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.scan_code),
            style = MaterialTheme.typography.h4.copy(
                color = Black,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier.size(140.dp),
            painter = painterResource(id = R.drawable.ic_scan),
            contentDescription = stringResource(R.string.gr_code)
        )
        DefaultButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.insert_code_manually),
            textColor = White,
            color = Black,
            onclick = { openDialog.value = true }
        )
    }
}

@Composable
private fun CustomDialog(
    openDialog: Boolean,
    onDismissDialog: () -> Unit,
    onJoinClicked: (String) -> Unit
) {
    var text by remember { mutableStateOf(EMPTY) }

    if (openDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.background,
            onDismissRequest = onDismissDialog,
            title = {
                Text(
                    text = stringResource(R.string.insert_code_manually),
                    style = MaterialTheme.typography.h6.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringResource(id = R.string.insert_game_code),
                        style = MaterialTheme.typography.body1
                    )
                    InfoTextField(
                        text = text,
                        label = stringResource(id = R.string.type_code),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        onValueChange = { text = it }
                    )
                }
            },
            buttons = {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DefaultButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp),
                        text = stringResource(R.string.join_game),
                        color = MaterialTheme.colors.onBackground,
                        onclick = {
                            if (text.isEmpty()) return@DefaultButton
                            onJoinClicked(text)
                        }
                    )
                    Text(
                        modifier = Modifier
                            .padding(20.dp)
                            .clickable { onDismissDialog() },
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.h6.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }
}
