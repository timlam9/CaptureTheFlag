package com.lamti.capturetheflag.presentation.ui.components.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.QrCodeAnalyzer
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun JoinGameScreen(onQrCodeScanned: (String) -> Unit) {
    val openDialog = remember { mutableStateOf(false) }

    QrCodeScanner()
    CustomDialog(
        openDialog = openDialog.value,
        onDismissDialog = { openDialog.value = false },
        onJoinClicked = onQrCodeScanned
    )

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { openDialog.value = true }
    ) {
        Text(stringResource(R.string.insert_game_code))
    }
}

@Composable
fun QrCodeScanner() {
    var code by remember { mutableStateOf(EMPTY) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (hasCamPermission) {
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context)
                    val preview = Preview.Builder().build()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(
                            Size(
                                previewView.width,
                                previewView.height
                            )
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer { result ->
                            Log.d("TAGARA", "Scan result: $result")
                            code = result
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
                        e.printStackTrace()
                    }
                    previewView
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = code,
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        }
    }
}

@Composable
fun CustomDialog(
    openDialog: Boolean,
    onDismissDialog: () -> Unit,
    onJoinClicked: (String) -> Unit
) {
    var text by remember { mutableStateOf(EMPTY) }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = {
                Text(text = stringResource(R.string.join_game))
            },
            text = {
                Column {
                    TextField(
                        value = text,
                        onValueChange = { text = it }
                    )
                    Text(stringResource(R.string.type_game_code))
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.padding(all = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onJoinClicked(text) }
                    ) {
                        Text(stringResource(R.string.join))
                    }
                }
            }
        )
    }
}
