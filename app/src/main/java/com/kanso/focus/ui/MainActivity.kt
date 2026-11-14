package com.kanso.focus.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kanso.focus.data.FocusSessionManager
import com.kanso.focus.service.FocusAccessibilityService
import com.kanso.focus.ui.theme.BorderZinc
import com.kanso.focus.ui.theme.KansoMinimalTheme
import com.kanso.focus.ui.theme.MutedGray
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
import com.kanso.focus.ui.theme.SurfaceZinc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusSessionManager.init(applicationContext)

        setContent {
            KansoMinimalTheme {
                KansoMainScreen()
            }
        }
    }
}

@Composable
fun KansoMainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Modo Enfoque, 1 = Lector Adaptado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Cabecera y selector superior entre "Modo Enfoque" y "Lector Adaptado"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KANSO",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceZinc)
                    .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TopTabItem(
                    title = "Modo Enfoque",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                TopTabItem(
                    title = "Lector Adaptado",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            FocusModeContent()
        } else {
            AdaptiveReaderScreen()
        }
    }
}

@Composable
private fun TopTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) PureWhite else SurfaceZinc)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) PureBlack else MutedGray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FocusModeContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val profiles by FocusSessionManager.profiles.collectAsState()
    val activeProfile by FocusSessionManager.activeProfile.collectAsState()
    val restrictedPackages by FocusSessionManager.restrictedPackages.collectAsState()
    val mutedPackages by FocusSessionManager.mutedPackages.collectAsState()
    val isSessionActive by FocusSessionManager.isSessionActive.collectAsState()
    val isStrictMode by FocusSessionManager.isStrictMode.collectAsState()
    val remainingSeconds by FocusSessionManager.remainingSeconds.collectAsState()
    val scheduleEnabled by FocusSessionManager.scheduleEnabled.collectAsState()
    val scheduleHour by FocusSessionManager.scheduleHour.collectAsState()
    val scheduleMinute by FocusSessionManager.scheduleMinute.collectAsState()

    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
                hasNotificationPermission = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    var showStrictTimerDialog by remember { mutableStateOf(false) }
    var strictMinutesInput by remember { mutableStateOf("25") }

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleHourInput by remember { mutableStateOf(scheduleHour.toString()) }
    var scheduleMinuteInput by remember { mutableStateOf(String.format("%02d", scheduleMinute)) }

    LaunchedEffect(Unit) {
        installedApps = loadLaunchableApps(context)
    }

    val filteredApps = remember(installedApps, searchQuery, restrictedPackages, mutedPackages) {
        val query = searchQuery.trim().lowercase()
        val matching = if (query.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
        matching.sortedWith(
            compareByDescending<InstalledAppInfo> {
                restrictedPackages.contains(it.packageName) || mutedPackages.contains(it.packageName)
            }.thenBy { it.label.lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Avisos de permisos que se ocultan automáticamente al activarse
        if (!hasAccessibilityPermission) {
            PermissionBanner(
                title = "Permiso de Accesibilidad desactivado",
                subtitle = "Toca para activar el bloqueo de apps restringidas.",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!hasNotificationPermission) {
            PermissionBanner(
                title = "Acceso a Notificaciones desactivado",
                subtitle = "Toca para permitir que Kanso silencie notificaciones.",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Barra horizontal de perfiles en un toque + programador diario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                val isSelected = profile == activeProfile
                val isCustom = profile !in listOf("Estudio", "Trabajo", "Descanso")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PureWhite else SurfaceZinc)
                        .border(1.dp, if (isSelected) PureWhite else BorderZinc, RoundedCornerShape(8.dp))
                        .clickable { FocusSessionManager.selectProfile(profile) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile,
                        color = if (isSelected) PureBlack else PureWhite,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isCustom && !isSessionActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Borrar perfil",
                            tint = if (isSelected) PureBlack else MutedGray,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { FocusSessionManager.deleteProfile(profile) }
                        )
                    }
                }
            }

            IconButton(
                onClick = { showAddProfileDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceZinc)
                    .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir perfil",
                    tint = PureWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { showScheduleDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (scheduleEnabled) PureWhite else SurfaceZinc)
                    .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Programar sesión diaria",
                    tint = if (scheduleEnabled) PureBlack else PureWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Panel de Estado / Botones de Inicio de Sesión
        if (isSessionActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceZinc)
                    .border(1.dp, PureWhite, RoundedCornerShape(10.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isStrictMode) {
                        "SESIÓN ESTRICTA ACTIVA · SIN SALIDA"
                    } else {
                        "SESIÓN MODO LIBRE ACTIVA"
                    },
                    color = MutedGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (remainingSeconds > 0L) {
                        FocusSessionManager.formatRemainingTime(remainingSeconds)
                    } else {
                        "ACTIVO"
                    },
                    color = PureWhite,
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isStrictMode) {
                    Text(
                        text = "El modo con temporizador impide cancelar la sesión antes de llegar a 00:00.",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                } else {
                    Button(
                        onClick = { FocusSessionManager.stopSession() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(
                            text = "Desactivar modo libre",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showStrictTimerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    )
                ) {
                    Text(
                        text = "Activar con temporizador (Sin salida)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = { FocusSessionManager.startFlexibleSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderZinc),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceZinc,
                        contentColor = PureWhite
                    )
                ) {
                    Text(
                        text = "Activar modo libre (Desactivar con botón)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Buscador de aplicaciones
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("Buscar aplicación...", color = MutedGray, fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MutedGray
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceZinc,
                unfocusedContainerColor = SurfaceZinc,
                focusedBorderColor = PureWhite,
                unfocusedBorderColor = BorderZinc,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                cursorColor = PureWhite
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "APLICACIONES · PERFIL ${activeProfile.uppercase()}",
                color = MutedGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${restrictedPackages.size} bloq · ${mutedPackages.size} sil",
                color = MutedGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de aplicaciones con campana (silenciar notificaciones) y candado (restringir acceso)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isMuted = mutedPackages.contains(app.packageName)
                val isRestricted = restrictedPackages.contains(app.packageName)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceZinc)
                        .border(
                            width = 1.dp,
                            color = if (isRestricted || isMuted) PureWhite.copy(alpha = 0.35f) else BorderZinc,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            color = MutedGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Campana independiente para silenciar notificaciones
                        IconButton(
                            onClick = { FocusSessionManager.toggleMutedPackage(app.packageName) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMuted) PureWhite else PureBlack)
                                .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = "Silenciar notificaciones",
                                tint = if (isMuted) PureBlack else MutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Candado independiente para restringir acceso a la app
                        IconButton(
                            onClick = { FocusSessionManager.toggleRestrictedPackage(app.packageName) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRestricted) PureWhite else PureBlack)
                                .border(1.dp, BorderZinc, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isRestricted) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Restringir apertura",
                                tint = if (isRestricted) PureBlack else MutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de temporizador estricto (Sin salida)
    if (showStrictTimerDialog) {
        AlertDialog(
            onDismissRequest = { showStrictTimerDialog = false },
            containerColor = SurfaceZinc,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = {
                Text("Temporizador estricto", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Elige los minutos de concentración. Una vez iniciado, no podrás cancelar la sesión hasta llegar a 00:00.",
                        color = MutedGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = strictMinutesInput,
                        onValueChange = { strictMinutesInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutos", color = MutedGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = BorderZinc,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = strictMinutesInput.toIntOrNull()?.coerceAtLeast(1) ?: 25
                        FocusSessionManager.startStrictTimedSession(mins)
                        showStrictTimerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Iniciar bloqueo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStrictTimerDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // Diálogo para añadir perfil personalizado
    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            containerColor = SurfaceZinc,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Nuevo perfil", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    placeholder = { Text("Ej. Lectura, Examen...", color = MutedGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = BorderZinc,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FocusSessionManager.addProfile(newProfileName)
                        newProfileName = ""
                        showAddProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // Diálogo de programación diaria con AlarmManager
    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            containerColor = SurfaceZinc,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Programador diario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Inicia automáticamente una sesión de enfoque cada día a la hora indicada.",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scheduleHourInput,
                            onValueChange = { scheduleHourInput = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Hora (0-23)", color = MutedGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = BorderZinc,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            )
                        )
                        OutlinedTextField(
                            value = scheduleMinuteInput,
                            onValueChange = { scheduleMinuteInput = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Min (0-59)", color = MutedGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = BorderZinc,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hr = (scheduleHourInput.toIntOrNull() ?: 9).coerceIn(0, 23)
                        val min = (scheduleMinuteInput.toIntOrNull() ?: 0).coerceIn(0, 59)
                        FocusSessionManager.scheduleDailySession(
                            context = context,
                            hourOfDay = hr,
                            minute = min,
                            durationMinutes = 45,
                            strictMode = true
                        )
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Activar diario", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        FocusSessionManager.cancelDailySchedule(context)
                        showScheduleDialog = false
                    }
                ) {
                    Text("Desactivar", color = MutedGray)
                }
            }
        )
    }
}

@Composable
private fun PermissionBanner(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceZinc)
            .border(1.dp, PureWhite.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MutedGray,
                fontSize = 12.sp
            )
        }
        Text(
            text = "Activar",
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':').any {
        ComponentName.unflattenFromString(it) == expectedComponentName
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
}

private suspend fun loadLaunchableApps(context: Context): List<InstalledAppInfo> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                InstalledAppInfo(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
