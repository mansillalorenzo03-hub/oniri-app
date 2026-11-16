package com.kanso.focus.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
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
import com.kanso.focus.ui.theme.BorderZinc
import com.kanso.focus.ui.theme.MutedGray
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
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

@Composable
fun AdaptiveReaderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(ReaderAccessibilityMode.BOTH) }
    var fontSizeSp by remember { mutableFloatStateOf(18f) }
    var highContrastYellow by remember { mutableStateOf(false) }
    var limitToThreePages by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }

    var documentTitle by remember { mutableStateOf("Texto de demostración Kanso") }
    var microBlocks by remember {
        mutableStateOf(
            splitIntoMicroBlocks(
                "Kanso divide documentos densos en micro-bloques numerados para reducir la fatiga cognitiva y visual. " +
                        "En el modo Dislexia se aplica lectura biónica resaltando la raíz inicial de cada palabra para guiar la fijación ocular. " +
                        "En el modo Poca Visión se amplía la escala tipográfica y todos los números como 2025, 15% o 3 páginas se destacan un 15% más grandes en negrita. " +
                        "Puedes subir cualquier archivo PDF o TXT, escucharlo en voz alta entre 1.0x y 1.75x o exportarlo en PDF adaptado."
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

    DisposableEffect(tts) {
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("block_")?.toIntOrNull()
                if (idx != null) {
                    val next = idx + 1
                    if (next < microBlocks.size) {
                        speakingBlockIndex = next
                        tts?.setSpeechRate(speechRate)
                        tts?.speak(
                            microBlocks[next],
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "block_$next"
                        )
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
    val activeCardBg = if (highContrastYellow) PureBlack else SurfaceZinc
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
        Text(
            text = "MODO DE ACCESIBILIDAD",
            color = MutedGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderAccessibilityMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PureWhite else SurfaceZinc)
                        .border(1.dp, if (isSelected) PureWhite else BorderZinc, RoundedCornerShape(8.dp))
                        .clickable { selectedMode = mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) PureBlack else PureWhite,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de carga de documento y controles visuales (A-, A+, Contraste Amarillo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    documentPickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Subir PDF o TXT",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Subir PDF / TXT", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(14f) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(32f) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = PureWhite
                )
            ) {
                Text("A+", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            IconButton(
                onClick = { highContrastYellow = !highContrastYellow },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (highContrastYellow) OpticalYellow else SurfaceZinc)
                    .border(1.dp, if (highContrastYellow) OpticalYellow else BorderZinc, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Contrast,
                    contentDescription = "Contraste Amarillo Óptico",
                    tint = if (highContrastYellow) PureBlack else OpticalYellow
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de TextToSpeech y velocidades (1.0x, 1.25x, 1.5x, 1.75x)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceZinc)
                .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!ttsReady || microBlocks.isEmpty()) return@IconButton
                        if (speakingBlockIndex != -1) {
                            tts?.stop()
                            speakingBlockIndex = -1
                        } else {
                            speakingBlockIndex = 0
                            tts?.setSpeechRate(speechRate)
                            tts?.speak(
                                microBlocks[0],
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "block_0"
                            )
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (speakingBlockIndex != -1) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Leer en voz alta",
                        tint = if (highContrastYellow) OpticalYellow else PureWhite
                    )
                }
                Text(
                    text = if (speakingBlockIndex != -1) "Leyendo #${speakingBlockIndex + 1}" else "Voz TTS",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speeds = listOf(1.0f, 1.25f, 1.5f, 1.75f)
                speeds.forEach { speed ->
                    val activeSpeed = speechRate == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeSpeed) PureWhite else PureBlack)
                            .border(1.dp, BorderZinc, RoundedCornerShape(6.dp))
                            .clickable {
                                speechRate = speed
                                tts?.setSpeechRate(speed)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (activeSpeed) PureBlack else MutedGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de exportación PDF con interruptor "Máx. 3 hojas"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceZinc)
                .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = limitToThreePages,
                    onCheckedChange = { limitToThreePages = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = PureWhite,
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
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrastYellow) OpticalYellow else PureWhite,
                    contentColor = PureBlack
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

        Spacer(modifier = Modifier.height(12.dp))

        // Encabezado del documento cargado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = documentTitle,
                color = MutedGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${microBlocks.size} bloques",
                color = MutedGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoadingFile) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PureWhite)
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeCardBg)
                            .border(
                                width = if (isBlockSpeaking) 2.dp else 1.dp,
                                color = if (isBlockSpeaking) OpticalYellow else activeBorderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (ttsReady) {
                                    speakingBlockIndex = index
                                    tts?.setSpeechRate(speechRate)
                                    tts?.speak(
                                        block,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "block_$index"
                                    )
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BLOQUE ${String.format("%02d", index + 1)}",
                                color = if (highContrastYellow) OpticalYellow else MutedGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Escuchar bloque",
                                tint = if (isBlockSpeaking) OpticalYellow else MutedGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
