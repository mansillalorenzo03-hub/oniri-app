package com.kanso.focus.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kanso.focus.ui.theme.AccentAmber
import com.kanso.focus.ui.theme.AccentCyan
import com.kanso.focus.ui.theme.AccentEmerald
import com.kanso.focus.ui.theme.AccentIndigo
import com.kanso.focus.ui.theme.AccentViolet
import com.kanso.focus.ui.theme.BorderZinc
import com.kanso.focus.ui.theme.MutedGray
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
import com.kanso.focus.ui.theme.SurfaceElevated
import com.kanso.focus.ui.theme.SurfaceZinc
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.ceil

val OpticalYellow = Color(0xFFFFD600)

data class KansoVoicePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val pitch: Float,
    val rateMultiplier: Float,
    val preferFemale: Boolean?,
    val accentColor: Color
)

private val voicePresets = listOf(
    KansoVoicePreset(
        id = "natural",
        name = "Humana",
        subtitle = "Natural cálida",
        pitch = 1.02f,
        rateMultiplier = 0.98f,
        preferFemale = true,
        accentColor = AccentEmerald
    ),
    KansoVoicePreset(
        id = "female",
        name = "Mujer",
        subtitle = "Clara y suave",
        pitch = 1.24f,
        rateMultiplier = 1.0f,
        preferFemale = true,
        accentColor = AccentViolet
    ),
    KansoVoicePreset(
        id = "deep",
        name = "Grave",
        subtitle = "Profunda",
        pitch = 0.74f,
        rateMultiplier = 0.94f,
        preferFemale = false,
        accentColor = AccentIndigo
    ),
    KansoVoicePreset(
        id = "narrator",
        name = "Narrador",
        subtitle = "Pausada",
        pitch = 0.88f,
        rateMultiplier = 0.88f,
        preferFemale = false,
        accentColor = AccentAmber
    ),
    KansoVoicePreset(
        id = "bright",
        name = "Dinámica",
        subtitle = "Brillante",
        pitch = 1.14f,
        rateMultiplier = 1.06f,
        preferFemale = true,
        accentColor = AccentCyan
    )
)

@Composable
fun AdaptiveReaderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(ReaderAccessibilityMode.BOTH) }
    var fontSizeSp by remember { mutableFloatStateOf(18f) }
    var highContrastYellow by remember { mutableStateOf(false) }
    var limitToThreePages by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var selectedVoicePreset by remember { mutableStateOf(voicePresets.first()) }

    var documentTitle by remember { mutableStateOf("Texto de demostración Kanso") }
    var microBlocks by remember {
        mutableStateOf(
            splitIntoMicroBlocks(
                "Kanso divide documentos densos en micro-bloques numerados para reducir la fatiga cognitiva y visual. " +
                        "En el modo Dislexia se aplica lectura biónica resaltando la raíz inicial de cada palabra para guiar la fijación ocular. " +
                        "En el modo Poca Visión se amplía la escala tipográfica y todos los números como 2025, 15% o 3 páginas se destacan un 15% más grandes en negrita. " +
                        "Elige entre las 5 voces arriba (Humana, Mujer, Grave, Narrador o Dinámica), sube cualquier PDF o TXT y expórtalo adaptado."
            )
        )
    }
    var isLoadingFile by remember { mutableStateOf(false) }
    var speakingBlockIndex by remember { mutableIntStateOf(-1) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale("es", "ES")
                ttsReady = true
            }
        }
        instance
    }

    fun speakBlockWithPreset(
        blockIndex: Int,
        preset: KansoVoicePreset = selectedVoicePreset,
        baseRate: Float = speechRate
    ) {
        if (!ttsReady || blockIndex !in microBlocks.indices) return
        val engine = tts ?: return
        applyVoicePresetToTts(engine, preset, baseRate)
        speakingBlockIndex = blockIndex
        engine.speak(
            microBlocks[blockIndex],
            TextToSpeech.QUEUE_FLUSH,
            null,
            "block_$blockIndex"
        )
    }

    DisposableEffect(tts, selectedVoicePreset, speechRate, microBlocks) {
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("block_")?.toIntOrNull()
                if (idx != null) {
                    val next = idx + 1
                    if (next < microBlocks.size) {
                        speakingBlockIndex = next
                        tts?.let { engine ->
                            applyVoicePresetToTts(engine, selectedVoicePreset, speechRate)
                            engine.speak(
                                microBlocks[next],
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "block_$next"
                            )
                        }
                    } else {
                        speakingBlockIndex = -1
                    }
                } else {
                    speakingBlockIndex = -1
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speakingBlockIndex = -1
            }
        }
        tts?.setOnUtteranceProgressListener(listener)

        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoadingFile = true
            scope.launch {
                val (name, extracted) = extractDocumentText(context, uri)
                documentTitle = name
                microBlocks = splitIntoMicroBlocks(extracted)
                isLoadingFile = false
            }
        }
    }

    val activeTextColor = if (highContrastYellow) OpticalYellow else PureWhite
    val activeCardBg = if (highContrastYellow) PureBlack else SurfaceElevated
    val activeBorderColor = if (highContrastYellow) OpticalYellow else BorderZinc

    val effectiveFontSize = when (selectedMode) {
        ReaderAccessibilityMode.DYSLEXIA -> fontSizeSp
        ReaderAccessibilityMode.LOW_VISION, ReaderAccessibilityMode.BOTH -> (fontSizeSp * 1.2f).coerceAtMost(38f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp)
    ) {
        // Selector de Modo: Solo Dislexia, Poca Visión, Ambas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceZinc)
                .border(1.dp, BorderZinc, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReaderAccessibilityMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentIndigo else Color.Transparent)
                        .clickable { selectedMode = mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) PureWhite else MutedGray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selector de 5 Voces distintas (Humana, Mujer, Grave, Narrador, Dinámica)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceZinc)
                .border(1.dp, BorderZinc, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voces",
                        tint = selectedVoicePreset.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TIPO DE VOZ (5 PERFILES)",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Selector de velocidad compacto
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1.0f, 1.25f, 1.5f, 1.75f).forEach { speed ->
                        val activeSpeed = speechRate == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeSpeed) selectedVoicePreset.accentColor else SurfaceElevated)
                                .clickable {
                                    speechRate = speed
                                    if (speakingBlockIndex != -1) {
                                        speakBlockWithPreset(speakingBlockIndex, selectedVoicePreset, speed)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (activeSpeed) PureBlack else MutedGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                voicePresets.forEach { preset ->
                    val isSelected = selectedVoicePreset.id == preset.id
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) preset.accentColor.copy(alpha = 0.2f)
                                else SurfaceElevated
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) preset.accentColor else BorderZinc,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedVoicePreset = preset
                                val targetBlock = if (speakingBlockIndex != -1) speakingBlockIndex else 0
                                speakBlockWithPreset(targetBlock, preset, speechRate)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(preset.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.name,
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = preset.subtitle,
                            color = MutedGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de controles: Subir PDF/TXT, Play/Stop, A-, A+, Contraste Amarillo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    if (speakingBlockIndex != -1) {
                        tts?.stop()
                        speakingBlockIndex = -1
                    } else {
                        speakBlockWithPreset(0, selectedVoicePreset, speechRate)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (speakingBlockIndex != -1) AccentAmber else AccentEmerald,
                    contentColor = PureBlack
                )
            ) {
                Icon(
                    imageVector = if (speakingBlockIndex != -1) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Escuchar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (speakingBlockIndex != -1) "Parar" else "Leer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    documentPickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Subir PDF o TXT",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "PDF / TXT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(14f) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Text("A-", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(32f) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Text("A+", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(
                onClick = { highContrastYellow = !highContrastYellow },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (highContrastYellow) OpticalYellow else SurfaceZinc)
                    .border(
                        1.dp,
                        if (highContrastYellow) OpticalYellow else BorderZinc,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Contrast,
                    contentDescription = "Contraste Amarillo Óptico",
                    tint = if (highContrastYellow) PureBlack else OpticalYellow
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de exportación PDF con interruptor "Máx. 3 hojas"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceZinc)
                .border(1.dp, BorderZinc, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = limitToThreePages,
                    onCheckedChange = { limitToThreePages = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = MutedGray,
                        uncheckedTrackColor = PureBlack
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Máx. 3 hojas",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    PdfExportHelper.exportAndShareAdaptedPdf(
                        context = context,
                        title = documentTitle,
                        blocks = microBlocks,
                        mode = selectedMode,
                        baseFontSizeSp = fontSizeSp,
                        highContrastYellow = highContrastYellow,
                        limitToThreePages = limitToThreePages
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrastYellow) OpticalYellow else AccentIndigo,
                    contentColor = if (highContrastYellow) PureBlack else PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Exportar PDF",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Exportar PDF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoadingFile) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(microBlocks) { index, block ->
                    val isBlockSpeaking = speakingBlockIndex == index
                    val styledBlock = remember(block, selectedMode) {
                        formatAdaptiveBlockAnnotated(block, selectedMode)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeCardBg)
                            .border(
                                width = if (isBlockSpeaking) 2.dp else 1.dp,
                                color = if (isBlockSpeaking) selectedVoicePreset.accentColor else activeBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                speakBlockWithPreset(index, selectedVoicePreset, speechRate)
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BLOQUE ${String.format("%02d", index + 1)} · VOZ ${selectedVoicePreset.name.uppercase()}",
                                color = if (highContrastYellow) OpticalYellow else selectedVoicePreset.accentColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Escuchar bloque",
                                tint = if (isBlockSpeaking) selectedVoicePreset.accentColor else MutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = styledBlock,
                            color = activeTextColor,
                            fontSize = effectiveFontSize.sp,
                            lineHeight = (effectiveFontSize * 1.45f).sp
                        )
                    }
                }
            }
        }
    }
}

private fun applyVoicePresetToTts(
    tts: TextToSpeech,
    preset: KansoVoicePreset,
    baseSpeechRate: Float
) {
    try {
        val availableVoices = tts.voices?.filter { voice ->
            voice.locale.language.startsWith("es", ignoreCase = true) && !voice.isNetworkConnectionRequired
        }.orEmpty().ifEmpty {
            tts.voices?.filter { it.locale.language.startsWith("es", ignoreCase = true) }.orEmpty()
        }

        if (availableVoices.isNotEmpty()) {
            val matchedVoice: Voice? = when (preset.preferFemale) {
                true -> availableVoices.firstOrNull { v ->
                    val n = v.name.lowercase()
                    n.contains("female") || n.contains("sfg") || n.contains("es-es-x-eea") ||
                            n.contains("es-us-x-sfb") || n.contains("femen") || n.contains("mujer")
                } ?: availableVoices.first()
                false -> availableVoices.firstOrNull { v ->
                    val n = v.name.lowercase()
                    n.contains("male") && !n.contains("female") ||
                            n.contains("eef") || n.contains("eed") || n.contains("sfd") ||
                            n.contains("mascul")
                } ?: availableVoices.last()
                null -> availableVoices.first()
            }
            if (matchedVoice != null) {
                tts.voice = matchedVoice
            }
        }
    } catch (_: Exception) {
        // Fallback al motor estándar con ajuste de pitch y velocidad
    }

    tts.setPitch(preset.pitch)
    tts.setSpeechRate((baseSpeechRate * preset.rateMultiplier).coerceIn(0.6f, 2.2f))
}

private fun formatAdaptiveBlockAnnotated(
    text: String,
    mode: ReaderAccessibilityMode
): AnnotatedString {
    val applyBionic = mode == ReaderAccessibilityMode.DYSLEXIA || mode == ReaderAccessibilityMode.BOTH
    val applyLowVisionNumbers = mode == ReaderAccessibilityMode.LOW_VISION || mode == ReaderAccessibilityMode.BOTH

    return buildAnnotatedString {
        val tokenRegex = Regex("\\S+|\\s+")
        tokenRegex.findAll(text).forEach { match ->
            val token = match.value
            if (token.isBlank()) {
                append(token)
            } else {
                val hasDigits = token.any { it.isDigit() }
                if (applyLowVisionNumbers && hasDigits) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 1.15.em
                        )
                    ) {
                        append(token)
                    }
                } else if (applyBionic) {
                    val boldCount = ceil(token.length * 0.45).toInt().coerceIn(1, token.length)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(token.substring(0, boldCount))
                    }
                    if (boldCount < token.length) {
                        append(token.substring(boldCount))
                    }
                } else {
                    append(token)
                }
            }
        }
    }
}

private fun splitIntoMicroBlocks(rawText: String): List<String> {
    val normalized = rawText.replace("\r\n", "\n").trim()
    if (normalized.isEmpty()) return listOf("El documento seleccionado no contiene texto extraíble.")

    val sentences = normalized
        .split(Regex("(?<=[.!?])\\s+|\\n{2,}"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val blocks = mutableListOf<String>()
    val current = StringBuilder()
    var wordCount = 0

    for (sentence in sentences) {
        val wordsInSentence = sentence.split(Regex("\\s+")).size
        if (wordCount + wordsInSentence > 36 && current.isNotEmpty()) {
            blocks.add(current.toString().trim())
            current.clear()
            wordCount = 0
        }
        if (current.isNotEmpty()) current.append(" ")
        current.append(sentence)
        wordCount += wordsInSentence
    }
    if (current.isNotEmpty()) {
        blocks.add(current.toString().trim())
    }
    return blocks
}

private suspend fun extractDocumentText(context: Context, uri: Uri): Pair<String, String> =
    withContext(Dispatchers.IO) {
        var displayName = "Documento"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx != -1 && cursor.moveToFirst()) {
                displayName = cursor.getString(nameIdx) ?: "Documento"
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: ""
        val isPdf = mimeType.contains("pdf", ignoreCase = true) ||
                displayName.endsWith(".pdf", ignoreCase = true)

        val text = try {
            if (isPdf) {
                PDFBoxResourceLoader.init(context.applicationContext)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper()
                        stripper.getText(document)
                    }
                } ?: ""
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: ""
            }
        } catch (e: Exception) {
            "Error al procesar el archivo ($displayName): ${e.localizedMessage ?: "formato no compatible"}"
        }

        displayName to text
    }
