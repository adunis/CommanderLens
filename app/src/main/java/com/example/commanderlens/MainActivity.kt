package com.example.commanderlens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

private val Bg = Color(0xFF0E1520)
private val Panel = Color(0xFF182435)
private val Soft = Color(0xFF25354A)
private val Mint = Color(0xFF77E5BF)
private val SecondaryText = Color(0xFFADBED0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Mint, onPrimary = Bg, background = Bg,
                    surface = Panel, onSurface = Color.White, secondary = SecondaryText
                )
            ) {
                val vm: DeckViewModel = viewModel()
                CommanderHome(vm)
            }
        }
    }
}

@Composable
private fun CommanderHome(vm: DeckViewModel) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    var manualText by remember { mutableStateOf("") }
    val cardCount = vm.totalCards()

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("COMMANDER", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("LENS", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$cardCount / 100", color = Mint, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${vm.uniqueCards()} unique", color = SecondaryText, fontSize = 12.sp)
            }
        }
        LinearProgressIndicator(
            progress = { (cardCount / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp)),
            color = Mint, trackColor = Soft
        )
        Spacer(Modifier.height(14.dp))

        Box(
            Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp))
                .background(Panel),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                CameraScanner(
                    onDetected = { vm.scanCandidate(it) },
                    onOcrText = { vm.reportOcr(it) },
                    modifier = Modifier.fillMaxSize()
                )
                if (!vm.scanning) {
                    Box(Modifier.fillMaxSize().background(Bg.copy(alpha = .67f)), contentAlignment = Alignment.Center) {
                        Text("SCANNING PAUSED", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera access is required for live scanning.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) {
                        Text("Enable camera")
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                vm.status,
                modifier = Modifier.weight(1f),
                color = if (vm.status.startsWith("✓")) Mint else SecondaryText,
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(9.dp))
            TextButton(onClick = { vm.changeScanning(!vm.scanning) }) {
                Text(if (vm.scanning) "Pause" else "Resume", color = Mint)
            }
        }
        Text(
            "Camera reads: ${vm.lastOcr}",
            color = SecondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Max nonbasic copies", color = SecondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            SmallChoice("1 · EDH", selected = vm.maxCopies == 1) { vm.changeMaxCopies(1) }
            Spacer(Modifier.width(6.dp))
            SmallChoice("2 · Custom", selected = vm.maxCopies == 2) { vm.changeMaxCopies(2) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Card language", color = SecondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            SmallChoice("Auto", selected = vm.scanLanguage == "auto") { vm.changeScanLanguage("auto") }
            Spacer(Modifier.width(4.dp))
            SmallChoice("EN", selected = vm.scanLanguage == "en") { vm.changeScanLanguage("en") }
            Spacer(Modifier.width(4.dp))
            SmallChoice("IT", selected = vm.scanLanguage == "it") { vm.changeScanLanguage("it") }
            Spacer(Modifier.width(4.dp))
            SmallChoice("ES", selected = vm.scanLanguage == "es") { vm.changeScanLanguage("es") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = manualText, onValueChange = { manualText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search card name", fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    vm.manualAdd(manualText)
                    manualText = ""
                }),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { vm.manualAdd(manualText); manualText = "" },
                enabled = manualText.isNotBlank() && !vm.lookingUp,
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp)
            ) { Text("Add") }
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR DECK", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.weight(1f))
            TextButton(enabled = vm.cards.isNotEmpty(), onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Commander deck", vm.exportText()))
            }) { Text("Copy", color = Mint) }
            TextButton(enabled = vm.cards.isNotEmpty(), onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Commander decklist")
                    putExtra(Intent.EXTRA_TEXT, vm.exportText())
                }
                context.startActivity(Intent.createChooser(intent, "Share Commander decklist"))
            }) { Text("Share", color = Mint) }
        }
        if (vm.cards.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No cards scanned yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Place a card in the guide and hold it steady.", color = SecondaryText, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
                items(vm.cards, key = { normalizeName(it.name) }) { entry ->
                    DeckCardRow(entry = entry, onMinus = { vm.decrement(entry) }, onPlus = { vm.increment(entry) })
                }
            }
        }
    }
}

@Composable
private fun SmallChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) Mint else Soft)
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Bg else Color.White, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun DeckCardRow(entry: DeckEntry, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(35.dp).height(41.dp).clip(RoundedCornerShape(8.dp))
            .background(Soft), contentAlignment = Alignment.Center) {
            Text("${entry.quantity}×", color = Mint, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(if (entry.isBasic) "Basic land · unlimited" else "Nonbasic",
                color = SecondaryText, fontSize = 10.sp)
        }
        TextButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text("−", fontSize = 24.sp, color = SecondaryText)
        }
        TextButton(onClick = onPlus, contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text("+", fontSize = 22.sp, color = Mint)
        }
    }
}
