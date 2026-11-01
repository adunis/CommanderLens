package com.example.commanderlens

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as MlKitText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private class LiveCardAnalyzer(
    private val accept: (String) -> Boolean,
    private val reportOcr: (String?) -> Unit
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val gate = ScanGate()
    private val processing = AtomicBoolean(false)
    private var lastAnalyzedAt = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (processing.get() || now - lastAnalyzedAt < 500) {
            proxy.close()
            return
        }
        if (!processing.compareAndSet(false, true)) {
            proxy.close()
            return
        }
        lastAnalyzedAt = now
        val mediaImage = proxy.image
        if (mediaImage == null) {
            processing.set(false)
            proxy.close()
            return
        }
        try {
            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            val uprightWidth = if (proxy.imageInfo.rotationDegrees % 180 == 0) proxy.width else proxy.height
            val uprightHeight = if (proxy.imageInfo.rotationDegrees % 180 == 0) proxy.height else proxy.width
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = selectTitleLine(result, uprightWidth, uprightHeight)
                    reportOcr(text)
                    val stableName = gate.observe(text)
                    if (stableName != null && accept(stableName)) gate.accepted(stableName)
                }
                .addOnCompleteListener {
                    processing.set(false)
                    proxy.close()
                }
        } catch (_: Exception) {
            processing.set(false)
            proxy.close()
        }
    }

    private fun selectTitleLine(text: MlKitText, width: Int, height: Int): String? {
        // Camera is portrait. The user centers ONE vertically oriented card in the guide.
        // Its name line should be near the top of that guide, not at the card's type line.
        return text.textBlocks.asSequence().flatMap { it.lines.asSequence() }
            .mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                val centerX = bounds.centerX().toFloat() / width
                val centerY = bounds.centerY().toFloat() / height
                val raw = line.text.trim().replace(Regex("\\s+"), " ")
                if (centerX !in 0.13f..0.87f || centerY !in 0.08f..0.37f) return@mapNotNull null
                if (raw.length !in 3..55 || raw.count(Char::isLetter) < 3) return@mapNotNull null
                if (raw.matches(Regex(".*[.!?]$"))) return@mapNotNull null
                if (raw.matches(Regex("(?i)^(Basic (Snow )?Land|Legendary Creature|Legendary Artifact|Artifact Creature|Enchantment Creature)( .*)?$"))) return@mapNotNull null
                // Favor the title line around 17% from the top of the camera frame.
                val rank = kotlin.math.abs(centerY - 0.17f) + if (raw.length < 5) 0.1f else 0f
                rank to raw
            }
            .minByOrNull { it.first }?.second
    }

    fun close() = recognizer.close()
}

@Composable
fun CameraScanner(
    onDetected: (String) -> Boolean,
    onOcrText: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = LocalLifecycleOwner.current
    // Use updated callback without rebinding the camera when the deck state changes.
    val latestCallback = rememberUpdatedState(onDetected)
    val latestOcrCallback = rememberUpdatedState(onOcrText)
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(owner, previewView) {
        val analyzer = LiveCardAnalyzer(
            accept = { candidate -> latestCallback.value(candidate) },
            reportOcr = { value -> latestOcrCallback.value(value) }
        )
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val disposed = AtomicBoolean(false)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            if (!disposed.get()) {
                try {
                    provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                    provider?.unbindAll()
                    provider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    android.util.Log.e("CommanderLens", "Camera binding failed", e)
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed.set(true)
            provider?.unbindAll()
            // A pending OCR task retains its recognizer until its callback completes.
            analyzer.close()
            executor.shutdown()
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.88f)
                .border(2.dp, Color(0xFF72E1BB), RoundedCornerShape(13.dp))
        ) {
            Box(
                Modifier.fillMaxWidth().padding(top = 25.dp).height(2.dp)
                    .background(Color(0xFF72E1BB).copy(alpha = .8f))
            )
        }
        Text(
            "ALIGN CARD INSIDE FRAME",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp),
            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
    }
}
