package com.kanso.focus.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.kanso.focus.ui.theme.OpticalYellow
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

data class KansoVoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val preferredRegions: List<String>,
    val preferMale: Boolean,
    val cadenceMultiplier: Float,
    val subtlePitch: Float,
    val accentColor: Color
)

private val voiceProfilesCatalog = listOf(
    KansoVoiceProfile(
        id = "humana",
        name = "Humana",
        description = "Voz natural cálida en español (es-419 / es-MX de alta fidelidad)",
        preferredRegions = listOf("419", "MX", "US", "ES"),
        preferMale = false,
        cadenceMultiplier = 1.00f,
        subtlePitch = 1.00f,
        accentColor = AccentEmerald
    ),
    KansoVoiceProfile(
        id = "mujer",
        name = "Mujer",
        description = "Voz femenina nativa de España (es-ES de alta claridad)",
        preferredRegions = listOf("ES", "MX", "US"),
        preferMale = false,
        cadenceMultiplier = 1.02f,
        subtlePitch = 1.05f,
        accentColor = AccentViolet
    ),
    KansoVoiceProfile(
        id = "grave",
        name = "Grave",
        description = "Voz masculina con resonancia baja real asignada por paquete masculino",
        preferredRegions = listOf("ES", "US", "MX", "AR"),
        preferMale = true,
        cadenceMultiplier = 0.95f,
        subtlePitch = 0.92f,
        accentColor = AccentIndigo
    ),
    KansoVoiceProfile(
        id = "narrador",
        name = "Narrador",
        description = "Voz masculina neutra y pausada para lectura guiada (es-AR / es-US)",
        preferredRegions = listOf("AR", "US", "419", "MX", "ES"),
        preferMale = true,
        cadenceMultiplier = 0.90f,
        subtlePitch = 0.96f,
        accentColor = AccentAmber
    ),
    KansoVoiceProfile(
        id = "juvenil",
        name = "Juvenil",
        description = "Voz alternativa con cadencia más ágil y dinámica",
        preferredRegions = listOf("MX", "ES", "US", "CO"),
        preferMale = false,
        cadenceMultiplier = 1.08f,
        subtlePitch = 1.08f,
        accentColor = AccentCyan
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveReaderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var selectedMode by remember { mutableStateOf(ReaderAccessibilityMode.BOTH) }
    var fontSizeSp by remember { mutableFloatStateOf(19f) }
    var highContrastYellow by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var selectedProfile by remember { mutableStateOf(voiceProfilesCatalog.first()) }

    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    var showExportPdfDialog by remember { mutableStateOf(false) }
    var limitToThreePages by remember { mutableStateOf(false) }

    var documentTitle by remember { mutableStateOf("Lectura Adaptada Kanso") }
    var microBlocks by remember {
        mutableStateOf(
            splitIntoMicroBlocks(
                "Kanso divide documentos densos en micro-bloques numerados para reducir la fatiga cognitiva y visual. " +
                        "En el modo Dislexia se aplica lectura biónica resaltando la raíz inicial de cada palabra para guiar la fijación ocular. " +
                        "En el modo Poca Visión se amplía la escala tipográfica y todos los números como 2025, 15% o 3 páginas se destacan un 15% más grandes en negrita. " +
                        "Usa el reproductor inferior para escuchar con sincronización Karaoke en vivo, o abre el botón de ajustes arriba a la derecha para cambiar entre las 5 voces neuronales reales."
            )
        )
    }

    var isLoadingFile by remember { mutableStateOf(false) }
    var speakingBlockIndex by remember { mutableIntStateOf(-1) }
    var pausedBlockIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    var ttsReady by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var mappedVoicesByProfile by remember { mutableStateOf<Map<String, Voice>>(emptyMap()) }
    var hasHighQualityVoices by remember { mutableStateOf(true) }

    // Inicialización priorizando el motor de Google TTS para máxima calidad neuronal
    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = instance ?: return@OnInitListener
                val mappingResult = mapRealHighFidelityVoices(engine)
                mappedVoicesByProfile = mappingResult.first
                hasHighQualityVoices = mappingResult.second
                ttsEngine = engine
                ttsReady = true
            }
        }

        instance = try {
            TextToSpeech(context.applicationContext, initListener, "com.google.android.tts")
        } catch (_: Exception) {
            TextToSpeech(context.applicationContext, initListener)
        }

        onDispose {
            instance?.stop()
            instance?.shutdown()
            ttsEngine = null
        }
    }

    // Auto-scroll suave tipo Karaoke cuando avanza el bloque en reproducción
    LaunchedEffect(speakingBlockIndex) {
        if (speakingBlockIndex in microBlocks.indices) {
            lazyListState.animateScrollToItem(speakingBlockIndex)
        }
    }

    fun speakBlock(
        blockIndex: Int,
        profile: KansoVoiceProfile = selectedProfile,
        rate: Float = speechRate
    ) {
        val engine = ttsEngine ?: return
        if (!ttsReady || blockIndex !in microBlocks.indices) return

        isPaused = false
        pausedBlockIndex = blockIndex
        speakingBlockIndex = blockIndex

        engine.stop()
        applyRealVoiceProfile(
            tts = engine,
            profile = profile,
            mappedVoice = mappedVoicesByProfile[profile.id],
            userSpeed = rate
        )

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.speak(
            microBlocks[blockIndex],
            TextToSpeech.QUEUE_FLUSH,
            params,
            "karaoke_${blockIndex}_${profile.id}"
        )
    }

    fun pauseSpeech() {
        val engine = ttsEngine ?: return
        if (speakingBlockIndex != -1) {
            pausedBlockIndex = speakingBlockIndex
            isPaused = true
            speakingBlockIndex = -1
            engine.stop()
        }
    }

    fun stopSpeech() {
        val engine = ttsEngine ?: return
        isPaused = false
        pausedBlockIndex = 0
        speakingBlockIndex = -1
        engine.stop()
    }

    DisposableEffect(ttsEngine, selectedProfile, speechRate, microBlocks) {
        val engine = ttsEngine ?: return@DisposableEffect onDispose {}
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = parseBlockIndexFromUtterance(utteranceId)
                if (idx != null) {
                    mainHandler.post {
                        speakingBlockIndex = idx
                        pausedBlockIndex = idx
                        isPaused = false
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                val idx = parseBlockIndexFromUtterance(utteranceId)
                mainHandler.post {
                    if (isPaused) return@post
                    if (idx != null) {
                        val next = idx + 1
                        if (next < microBlocks.size) {
                            speakBlock(next, selectedProfile, speechRate)
                        } else {
                            speakingBlockIndex = -1
                            pausedBlockIndex = 0
                        }
                    } else {
                        speakingBlockIndex = -1
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    speakingBlockIndex = -1
                }
            }
        }
        engine.setOnUtteranceProgressListener(listener)
        onDispose {}
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            stopSpeech()
            isLoadingFile = true
            scope.launch {
                val (name, extracted) = extractDocumentText(context, uri)
                documentTitle = name
                microBlocks = splitIntoMicroBlocks(extracted)
                isLoadingFile = false
            }
        }
    }

    val effectiveFontSize = when (selectedMode) {
        ReaderAccessibilityMode.DYSLEXIA -> fontSizeSp
        ReaderAccessibilityMode.LOW_VISION, ReaderAccessibilityMode.BOTH -> (fontSizeSp * 1.18f).coerceAtMost(36f)
    }

    Scaffold(
        containerColor = PureBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Barra de acciones superior limpia y sin textos truncados
            Surface(
                color = SurfaceZinc,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderZinc),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceElevated,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Subir PDF o TXT",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subir PDF / TXT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { showExportPdfDialog = true },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentIndigo,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar PDF",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exportar PDF",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = { showSettingsBottomSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, OpticalYellow.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ajustes de voz y lectura",
                            tint = OpticalYellow
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Player persistente estilo reproductor de audio
            PersistentAudioBottomPlayer(
                activeProfile = selectedProfile,
                isPlaying = speakingBlockIndex != -1,
                isPaused = isPaused,
                currentBlockNumber = when {
                    speakingBlockIndex != -1 -> speakingBlockIndex + 1
                    isPaused -> pausedBlockIndex + 1
                    else -> 1
                },
                totalBlocks = microBlocks.size,
                speechRate = speechRate,
                onPlayClick = {
                    val target = if (speakingBlockIndex != -1) {
                        speakingBlockIndex
                    } else {
                        pausedBlockIndex.coerceIn(0, (microBlocks.size - 1).coerceAtLeast(0))
                    }
                    speakBlock(target, selectedProfile, speechRate)
                },
                onPauseClick = { pauseSpeech() },
                onStopClick = { stopSpeech() },
                onSpeedSelect = { newSpeed ->
                    speechRate = newSpeed
                    if (speakingBlockIndex != -1) {
                        speakBlock(speakingBlockIndex, selectedProfile, newSpeed)
                    }
                },
                onOpenVoiceSheet = { showSettingsBottomSheet = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            // Sub-barra informativa del documento actual y modo activo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = documentTitle,
                    color = MutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${selectedMode.label} · ${microBlocks.size} bloques",
                    color = OpticalYellow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoadingFile) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OpticalYellow)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = microBlocks,
                        key = { idx, _ -> idx }
                    ) { index, block ->
                        // Karaoke / Utterance Sync: resalta en tiempo real el bloque que se está leyendo con #FFD600
                        val isKaraokeActive = speakingBlockIndex == index
                        val styledBlock = remember(block, selectedMode) {
                            formatAdaptiveBlockAnnotated(block, selectedMode)
                        }

                        val cardBgColor = when {
                            isKaraokeActive -> Color(0xFF232114)
                            highContrastYellow -> PureBlack
                            else -> SurfaceZinc
                        }
                        val cardBorderColor = when {
                            isKaraokeActive -> OpticalYellow
                            highContrastYellow -> OpticalYellow.copy(alpha = 0.7f)
                            else -> BorderZinc
                        }
                        val blockTextColor = when {
                            isKaraokeActive -> OpticalYellow
                            highContrastYellow -> OpticalYellow
                            else -> PureWhite
                        }

                        Card(
                            onClick = { speakBlock(index, selectedProfile, speechRate) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(
                                width = if (isKaraokeActive) 2.dp else 1.dp,
                                color = cardBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isKaraokeActive) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = "Leyendo en vivo",
                                                tint = OpticalYellow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (isKaraokeActive) {
                                                "LEYENDO BLOQUE ${String.format("%02d", index + 1)}"
                                            } else {
                                                "BLOQUE ${String.format("%02d", index + 1)}"
                                            },
                                            color = if (isKaraokeActive || highContrastYellow) OpticalYellow else MutedGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Tocar para leer desde aquí",
                                        tint = if (isKaraokeActive) OpticalYellow else MutedGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = styledBlock,
                                    color = blockTextColor,
                                    fontSize = effectiveFontSize.sp,
                                    lineHeight = (effectiveFontSize * 1.5f).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ModalBottomSheet con configuración detallada de las 5 voces TTS reales, modos y contraste/tipografía
    if (showSettingsBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceZinc,
            contentColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajustes del Lector y Voces TTS",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // 1. Modos de Accesibilidad
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MODO DE LECTURA",
                        color = MutedGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderAccessibilityMode.values().forEach { mode ->
                            val selected = selectedMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { selectedMode = mode },
                                label = {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpticalYellow,
                                    selectedLabelColor = PureBlack,
                                    containerColor = SurfaceElevated,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }

                // 2. Controles de Escala Tipográfica (A- / A+) y Alto Contraste Amarillo Óptico
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(14f) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderZinc),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceElevated,
                                contentColor = PureWhite
                            )
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${fontSizeSp.toInt()} sp",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(32f) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderZinc),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceElevated,
                                contentColor = PureWhite
                            )
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold)
                        }
                    }

                    FilterChip(
                        selected = highContrastYellow,
                        onClick = { highContrastYellow = !highContrastYellow },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = null,
                                tint = if (highContrastYellow) PureBlack else OpticalYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Contraste #FFD600",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OpticalYellow,
                            selectedLabelColor = PureBlack,
                            containerColor = SurfaceElevated,
                            labelColor = PureWhite
                        )
                    )
                }

                HorizontalDivider(color = BorderZinc)

                // 3. Selector visual de los 5 perfiles de voz TTS reales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PERFILES DE VOZ NEURONAL (5 VOCES)",
                        color = MutedGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { promptInstallHighFidelityTtsData(context) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = OpticalYellow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Instalar voces HD",
                            color = OpticalYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!hasHighQualityVoices) {
                    Surface(
                        color = AccentAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { promptInstallHighFidelityTtsData(context) }
                    ) {
                        Text(
                            text = "Tu teléfono solo tiene voces básicas instaladas. Toca aquí para descargar los paquetes de voz neuronal de alta calidad de Google TTS.",
                            color = PureWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    voiceProfilesCatalog.forEach { profile ->
                        val isSelected = selectedProfile.id == profile.id
                        val assignedVoice = mappedVoicesByProfile[profile.id]
                        val voiceBadge = assignedVoice?.let {
                            "${it.locale.language}-${it.locale.country}"
                        } ?: profile.preferredRegions.first()

                        Card(
                            onClick = {
                                selectedProfile = profile
                                val targetBlock = if (speakingBlockIndex != -1) {
                                    speakingBlockIndex
                                } else {
                                    pausedBlockIndex.coerceIn(0, (microBlocks.size - 1).coerceAtLeast(0))
                                }
                                speakBlock(targetBlock, profile, speechRate)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    profile.accentColor.copy(alpha = 0.18f)
                                } else {
                                    SurfaceElevated
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) profile.accentColor else BorderZinc
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(profile.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                color = PureWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = voiceBadge,
                                                color = profile.accentColor,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = profile.description,
                                            color = MutedGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionada",
                                        tint = profile.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Diálogo Modal de Exportación PDF con el switch "Máx. 3 hojas" adentro
    if (showExportPdfDialog) {
        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = {
                Text(
                    text = "Exportar PDF Adaptado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Se generará un documento PDF con el modo \"${selectedMode.label}\" listo para imprimir o compartir.",
                        color = MutedGray,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceZinc)
                            .border(1.dp, BorderZinc, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limitar a máx. 3 hojas",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ajusta automáticamente la letra para que todo entre en 3 páginas",
                                color = MutedGray,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = limitToThreePages,
                            onCheckedChange = { limitToThreePages = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = OpticalYellow,
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = PureBlack
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportPdfDialog = false
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpticalYellow,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Generar y Compartir", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPdfDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }
}

@Composable
private fun PersistentAudioBottomPlayer(
    activeProfile: KansoVoiceProfile,
    isPlaying: Boolean,
    isPaused: Boolean,
    currentBlockNumber: Int,
    totalBlocks: Int,
    speechRate: Float,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    onOpenVoiceSheet: () -> Unit
) {
    Surface(
        color = SurfaceZinc,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, BorderZinc),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila superior del reproductor: Indicador de Voz Activa + Selector rápido de velocidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, activeProfile.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onOpenVoiceSheet() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voz activa",
                        tint = activeProfile.accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voz: ${activeProfile.name}",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1.0f, 1.25f, 1.5f, 1.75f).forEach { speed ->
                        val isSelectedSpeed = speechRate == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectedSpeed) OpticalYellow else SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelectedSpeed) OpticalYellow else BorderZinc,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSpeedSelect(speed) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (isSelectedSpeed) PureBlack else PureWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Fila inferior del reproductor: Estado de bloque + Controles horizontales Play, Pause, Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isPlaying -> "Reproduciendo bloque $currentBlockNumber de $totalBlocks"
                            isPaused -> "En pausa · Bloque $currentBlockNumber de $totalBlocks"
                            else -> "Listo para reproducir ($totalBlocks bloques)"
                        },
                        color = if (isPlaying) OpticalYellow else PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Play
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) AccentEmerald else OpticalYellow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = PureBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón Pause
                    IconButton(
                        onClick = onPauseClick,
                        enabled = isPlaying,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) SurfaceElevated else SurfaceElevated.copy(alpha = 0.5f))
                            .border(1.dp, BorderZinc, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pausar",
                            tint = if (isPlaying) PureWhite else MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botón Stop
                    IconButton(
                        onClick = onStopClick,
                        enabled = isPlaying || isPaused,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying || isPaused) SurfaceElevated else SurfaceElevated.copy(alpha = 0.5f))
                            .border(1.dp, BorderZinc, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener",
                            tint = if (isPlaying || isPaused) AccentAmber else MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Consulta `textToSpeech.voices`, filtra las voces en español priorizando
 * `Voice.QUALITY_VERY_HIGH` y `Voice.QUALITY_HIGH`, y asigna objetos `Voice` reales
 * distintos a cada uno de los 5 perfiles.
 */
private fun mapRealHighFidelityVoices(
    tts: TextToSpeech
): Pair<Map<String, Voice>, Boolean> {
    return try {
        val allSpanishVoices = tts.voices
            ?.filter { voice ->
                voice.locale.language.equals("es", ignoreCase = true) ||
                        voice.locale.language.startsWith("es", ignoreCase = true)
            }
            .orEmpty()

        val highQualityVoices = allSpanishVoices.filter { voice ->
            voice.quality >= Voice.QUALITY_HIGH && !voice.isNetworkConnectionRequired
        }.ifEmpty {
            allSpanishVoices.filter { it.quality >= Voice.QUALITY_HIGH }
        }

        val candidatePool = highQualityVoices.ifEmpty { allSpanishVoices }
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name }
            )

        val usedVoiceNames = mutableSetOf<String>()
        val result = mutableMapOf<String, Voice>()

        for (profile in voiceProfilesCatalog) {
            if (candidatePool.isEmpty()) break

            val chosen = candidatePool.firstOrNull { v ->
                v.name !in usedVoiceNames &&
                        matchesGenderPreference(v, profile.preferMale) &&
                        profile.preferredRegions.any { reg ->
                            v.locale.country.equals(reg, ignoreCase = true) ||
                                    v.name.contains(reg, ignoreCase = true)
                        }
            } ?: candidatePool.firstOrNull { v ->
                v.name !in usedVoiceNames && matchesGenderPreference(v, profile.preferMale)
            } ?: candidatePool.firstOrNull { v ->
                v.name !in usedVoiceNames
            } ?: candidatePool.first()

            usedVoiceNames.add(chosen.name)
            result[profile.id] = chosen
        }

        result to (highQualityVoices.size >= 2)
    } catch (_: Exception) {
        emptyMap<String, Voice>() to false
    }
}

private fun matchesGenderPreference(voice: Voice, preferMale: Boolean): Boolean {
    val n = voice.name.lowercase()
    val features = voice.features?.map { it.lowercase() }.orEmpty()
    val isExplicitMale = (n.contains("male") && !n.contains("female")) ||
            n.contains("-eef") || n.contains("-eed") || n.contains("-sfd") ||
            n.contains("-esd") || n.contains("mascul") ||
            features.any { it.contains("male") && !it.contains("female") }
    val isExplicitFemale = n.contains("female") ||
            n.contains("-eea") || n.contains("-efc") || n.contains("-sfb") ||
            n.contains("-sfg") || n.contains("-esf") || n.contains("femen") ||
            features.any { it.contains("female") }

    return if (preferMale) {
        isExplicitMale || !isExplicitFemale
    } else {
        isExplicitFemale || !isExplicitMale
    }
}

private fun applyRealVoiceProfile(
    tts: TextToSpeech,
    profile: KansoVoiceProfile,
    mappedVoice: Voice?,
    userSpeed: Float
) {
    try {
        if (mappedVoice != null) {
            tts.voice = mappedVoice
        } else {
            val fallbackCountry = profile.preferredRegions.firstOrNull() ?: "ES"
            tts.language = Locale("es", fallbackCountry)
        }
    } catch (_: Exception) {
    }

    tts.setPitch(profile.subtlePitch)
    tts.setSpeechRate((userSpeed * profile.cadenceMultiplier).coerceIn(0.75f, 2.0f))
}

/**
 * Dispara el Intent oficial de Android para instalar paquetes de voz TTS de alta fidelidad.
 */
fun promptInstallHighFidelityTtsData(context: Context) {
    try {
        val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    } catch (_: Exception) {
    }
}

private fun parseBlockIndexFromUtterance(utteranceId: String?): Int? {
    if (utteranceId == null || !utteranceId.startsWith("karaoke_")) return null
    return utteranceId.removePrefix("karaoke_").substringBefore("_").toIntOrNull()
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
