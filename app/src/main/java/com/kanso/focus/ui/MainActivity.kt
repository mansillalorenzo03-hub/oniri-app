package com.kanso.focus.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kanso.focus.data.FocusSessionManager
import com.kanso.focus.service.FocusAccessibilityService
import com.kanso.focus.ui.theme.AccentAmber
import com.kanso.focus.ui.theme.AccentCoral
import com.kanso.focus.ui.theme.AccentCyan
import com.kanso.focus.ui.theme.AccentEmerald
import com.kanso.focus.ui.theme.AccentIndigo
import com.kanso.focus.ui.theme.AccentViolet
import com.kanso.focus.ui.theme.BorderZinc
import com.kanso.focus.ui.theme.KansoMinimalTheme
import com.kanso.focus.ui.theme.MutedGray
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
import com.kanso.focus.ui.theme.SurfaceElevated
import com.kanso.focus.ui.theme.SurfaceZinc
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val iconBitmap: ImageBitmap?
)

enum class AppGridPickerType {
    NONE,
    BLOCK_APPS,
    MUTE_NOTIFICATIONS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusSessionManager.init(applicationContext)

        setContent {
            KansoMinimalTheme {
                KansoRootExperience()
            }
        }
    }
}

@Composable
fun KansoRootExperience() {
    val isSessionActive by FocusSessionManager.isSessionActive.collectAsState()
    var hasSelectedProfileOnLaunch by remember { mutableStateOf(isSessionActive) }

    if (!hasSelectedProfileOnLaunch) {
        NetflixProfileGatewayScreen(
            onProfilePicked = { profileName ->
                FocusSessionManager.selectProfile(profileName)
                hasSelectedProfileOnLaunch = true
            }
        )
    } else {
        KansoMainScreen(
            onSwitchProfileClicked = {
                hasSelectedProfileOnLaunch = false
            }
        )
    }
}

@Composable
fun NetflixProfileGatewayScreen(
    onProfilePicked: (String) -> Unit
) {
    val context = LocalContext.current
    val profiles by FocusSessionManager.profiles.collectAsState()
    val profileImageVersion by FocusSessionManager.profileImageVersion.collectAsState()

    var isManageMode by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileImageUri by remember { mutableStateOf<Uri?>(null) }

    var profilePendingDeletion by remember { mutableStateOf<String?>(null) }
    var profilePendingPhotoUpdate by remember { mutableStateOf<String?>(null) }

    val existingProfilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val targetProfile = profilePendingPhotoUpdate
        if (uri != null && targetProfile != null) {
            FocusSessionManager.setProfileImageFromUri(context, targetProfile, uri)
        }
        profilePendingPhotoUpdate = null
    }

    val newProfilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            newProfileImageUri = uri
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PureBlack, SurfaceZinc, PureBlack)
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Logo K blanca sobre fondo negro
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.5.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    color = PureWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "KANSO",
                color = PureWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 5.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isManageMode) "Administrar perfiles" else "¿Quién se va a concentrar?",
                color = PureWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isManageMode) {
                    "Toca la cámara para poner foto o la papelera para borrar un perfil"
                } else {
                    "Toca un perfil para entrar o el icono de cámara para ponerle tu foto"
                },
                color = MutedGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(profiles, key = { it }) { profile ->
                    val canDelete = profiles.size > 1
                    val avatarBitmap = remember(profile, profileImageVersion) {
                        FocusSessionManager.getProfileAvatar(profile)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.size(124.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Tarjeta cuadrada redondeada del perfil
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Brush.linearGradient(profileGradientFor(profile)))
                                    .border(
                                        width = 2.dp,
                                        color = if (isManageMode) AccentCoral else PureWhite.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .clickable {
                                        if (isManageMode) {
                                            if (canDelete) {
                                                profilePendingDeletion = profile
                                            }
                                        } else {
                                            onProfilePicked(profile)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap,
                                        contentDescription = profile,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = profile.take(2).uppercase(),
                                        color = PureWhite,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (isManageMode && canDelete) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PureBlack.copy(alpha = 0.58f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(AccentCoral),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar perfil",
                                                tint = PureWhite,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Botón flotante de cámara en la esquina inferior derecha para poner/cambiar foto
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceElevated)
                                    .border(1.5.dp, PureWhite.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        profilePendingPhotoUpdate = profile
                                        existingProfilePhotoLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Cambiar foto de $profile",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = profile,
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tarjeta para Añadir Perfil
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(SurfaceElevated)
                                .border(2.dp, BorderZinc, RoundedCornerShape(26.dp))
                                .clickable {
                                    newProfileName = ""
                                    newProfileImageUri = null
                                    showAddProfileDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nuevo perfil",
                                tint = PureWhite,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Añadir perfil",
                            color = MutedGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { isManageMode = !isManageMode },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isManageMode) AccentEmerald else BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isManageMode) AccentEmerald.copy(alpha = 0.16f) else SurfaceElevated,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = if (isManageMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isManageMode) "Listo" else "Administrar / Borrar perfiles",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Crear nuevo perfil", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text("Ej. Universidad, Gym, Lectura...", color = MutedGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = BorderZinc,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )

                    OutlinedButton(
                        onClick = { newProfilePhotoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (newProfileImageUri != null) AccentEmerald else BorderZinc
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceZinc,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Foto",
                            tint = if (newProfileImageUri != null) AccentEmerald else PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (newProfileImageUri != null) "Imagen seleccionada ✓" else "Elegir imagen de galería",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            FocusSessionManager.addProfile(
                                name = newProfileName,
                                context = context,
                                imageUri = newProfileImageUri
                            )
                            newProfileName = ""
                            newProfileImageUri = null
                            showAddProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentIndigo,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Crear perfil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    if (profilePendingDeletion != null) {
        val target = profilePendingDeletion!!
        AlertDialog(
            onDismissRequest = { profilePendingDeletion = null },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("¿Eliminar perfil \"$target\"?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se borrará este perfil junto con su foto y su configuración de aplicaciones bloqueadas y silenciadas.",
                    color = MutedGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FocusSessionManager.deleteProfile(target)
                        profilePendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCoral,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profilePendingDeletion = null }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }
}

@Composable
fun KansoMainScreen(
    onSwitchProfileClicked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Modo Enfoque, 1 = Lector Adaptado

    val activeProfile by FocusSessionManager.activeProfile.collectAsState()
    val profileImageVersion by FocusSessionManager.profileImageVersion.collectAsState()
    val activeAvatar = remember(activeProfile, profileImageVersion) {
        FocusSessionManager.getProfileAvatar(activeProfile)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceZinc,
                drawerContentColor = PureWhite,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Cabecera del menú lateral con Logo K y Perfil Activo con su foto
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.5.dp, PureWhite, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "K",
                                    color = PureWhite,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column {
                                Text(
                                    text = "KANSO",
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    text = "Menú de navegación",
                                    color = MutedGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Tarjeta del perfil activo dentro del menú lateral
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, BorderZinc, RoundedCornerShape(16.dp))
                                .clickable {
                                    scope.launch {
                                        drawerState.close()
                                        onSwitchProfileClicked()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(profileGradientFor(activeProfile))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeAvatar != null) {
                                        Image(
                                            bitmap = activeAvatar,
                                            contentDescription = activeProfile,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = activeProfile.take(2).uppercase(),
                                            color = PureWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = activeProfile,
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Toca para cambiar perfil",
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Cambiar perfil",
                                tint = AccentIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = BorderZinc)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SECCIONES DE LA APP",
                            color = MutedGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(
                                        text = "Modo Enfoque",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Bloqueo de apps y notificaciones",
                                        fontSize = 11.sp,
                                        color = MutedGray
                                    )
                                }
                            },
                            selected = selectedSection == 0,
                            onClick = {
                                selectedSection = 0
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Modo Enfoque"
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AccentIndigo.copy(alpha = 0.22f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = AccentIndigo,
                                unselectedIconColor = MutedGray,
                                selectedTextColor = PureWhite,
                                unselectedTextColor = PureWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(
                                        text = "Lector Adaptado",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Dislexia, Poca Visión y 5 Voces",
                                        fontSize = 11.sp,
                                        color = MutedGray
                                    )
                                }
                            },
                            selected = selectedSection == 1,
                            onClick = {
                                selectedSection = 1
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Lector Adaptado"
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AccentIndigo.copy(alpha = 0.22f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = AccentIndigo,
                                unselectedIconColor = MutedGray,
                                selectedTextColor = PureWhite,
                                unselectedTextColor = PureWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onSwitchProfileClicked()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderZinc),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceElevated,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ir a pantalla de perfiles", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            // Barra Superior con el botón de las 3 barritas (☰) en la esquina izquierda
            // y el avatar del perfil con imagen en la esquina derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón de las 3 barritas (Menú Hamburguesa) en la esquina
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderZinc, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir menú de 3 barritas",
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (selectedSection == 0) "Modo Enfoque" else "Lector Adaptado",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Perfil $activeProfile",
                            color = MutedGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Avatar del perfil en la esquina derecha con su foto personalizada
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderZinc, RoundedCornerShape(14.dp))
                        .clickable { onSwitchProfileClicked() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(profileGradientFor(activeProfile))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeAvatar != null) {
                            Image(
                                bitmap = activeAvatar,
                                contentDescription = activeProfile,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = activeProfile.take(2).uppercase(),
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeProfile,
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedSection == 0) {
                FocusModeDashboard()
            } else {
                AdaptiveReaderScreen()
            }
        }
    }
}

@Composable
private fun FocusModeDashboard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeProfile by FocusSessionManager.activeProfile.collectAsState()
    val restrictedPackages by FocusSessionManager.restrictedPackages.collectAsState()
    val mutedPackages by FocusSessionManager.mutedPackages.collectAsState()
    val installedApps by FocusSessionManager.installedApps.collectAsState()
    val isSessionActive by FocusSessionManager.isSessionActive.collectAsState()
    val isStrictMode by FocusSessionManager.isStrictMode.collectAsState()
    val remainingSeconds by FocusSessionManager.remainingSeconds.collectAsState()
    val scheduleEnabled by FocusSessionManager.scheduleEnabled.collectAsState()
    val scheduleHour by FocusSessionManager.scheduleHour.collectAsState()
    val scheduleMinute by FocusSessionManager.scheduleMinute.collectAsState()

    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var hasUsageStatsPermission by remember {
        mutableStateOf(FocusSessionManager.hasUsageStatsPermission(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
                hasUsageStatsPermission = FocusSessionManager.hasUsageStatsPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasNotificationPermission = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeGridPicker by remember { mutableStateOf(AppGridPickerType.NONE) }
    var showPermissionsSheet by remember { mutableStateOf(false) }

    var showStrictTimerDialog by remember { mutableStateOf(false) }
    var strictMinutesInput by remember { mutableStateOf("25") }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleHourInput by remember { mutableStateOf(scheduleHour.toString()) }
    var scheduleMinuteInput by remember { mutableStateOf(String.format("%02d", scheduleMinute)) }

    val canEnforceAppBlocking = hasAccessibilityPermission || (hasUsageStatsPermission && hasOverlayPermission)

    if (activeGridPicker != AppGridPickerType.NONE) {
        AppGridPickerModal(
            pickerType = activeGridPicker,
            installedApps = installedApps,
            selectedPackages = if (activeGridPicker == AppGridPickerType.BLOCK_APPS) {
                restrictedPackages
            } else {
                mutedPackages
            },
            onTogglePackage = { pkg ->
                if (activeGridPicker == AppGridPickerType.BLOCK_APPS) {
                    FocusSessionManager.toggleRestrictedPackage(pkg)
                } else {
                    FocusSessionManager.toggleMutedPackage(pkg)
                }
            },
            onClose = { activeGridPicker = AppGridPickerType.NONE }
        )
        return
    }

    // Contenedor con scroll vertical fluido y excelente espaciado para que nada quede apretado
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Banner de permisos si falta alguno
        if (!canEnforceAppBlocking || !hasNotificationPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AccentAmber.copy(alpha = 0.14f))
                    .border(1.dp, AccentAmber.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .clickable { showPermissionsSheet = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Permisos",
                        tint = AccentAmber,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Faltan permisos de bloqueo",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Toca aquí para habilitar el bloqueo de apps y notificaciones",
                            color = MutedGray,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentAmber)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Activar",
                        color = PureBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Tarjeta Principal de Encendido / Apagado (Power Hero Card) con amplio espacio interior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isSessionActive) {
                        Brush.linearGradient(
                            listOf(
                                AccentEmerald.copy(alpha = 0.22f),
                                SurfaceElevated
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                SurfaceElevated,
                                SurfaceZinc
                            )
                        )
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSessionActive) AccentEmerald else BorderZinc,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSessionActive) AccentEmerald else MutedGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSessionActive) {
                                if (isStrictMode) "ENCENDIDO · MODO ESTRICTO" else "ENCENDIDO · MODO LIBRE"
                            } else {
                                "APAGADO · LISTO PARA ENFOCAR"
                            },
                            color = if (isSessionActive) AccentEmerald else MutedGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Switch(
                        checked = isSessionActive,
                        onCheckedChange = { wantOn ->
                            if (wantOn) {
                                if (!canEnforceAppBlocking && restrictedPackages.isNotEmpty()) {
                                    showPermissionsSheet = true
                                } else {
                                    FocusSessionManager.startFlexibleSession()
                                }
                            } else {
                                if (!isStrictMode) {
                                    FocusSessionManager.stopSession()
                                }
                            }
                        },
                        enabled = !isStrictMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = AccentEmerald,
                            uncheckedThumbColor = MutedGray,
                            uncheckedTrackColor = PureBlack
                        )
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Botón Circular Grande Tipo Power ON / OFF
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSessionActive) {
                                Brush.radialGradient(
                                    listOf(AccentEmerald, AccentEmerald.copy(alpha = 0.35f))
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(AccentIndigo, SurfaceZinc)
                                )
                            }
                        )
                        .border(
                            width = 3.dp,
                            color = if (isSessionActive) AccentEmerald else AccentIndigo.copy(alpha = 0.75f),
                            shape = CircleShape
                        )
                        .clickable {
                            if (isSessionActive) {
                                if (!isStrictMode) {
                                    FocusSessionManager.stopSession()
                                }
                            } else {
                                if (!canEnforceAppBlocking && restrictedPackages.isNotEmpty()) {
                                    showPermissionsSheet = true
                                } else {
                                    FocusSessionManager.startFlexibleSession()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Encender o Apagar",
                            tint = PureWhite,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSessionActive) "ON" else "OFF",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isSessionActive && remainingSeconds > 0L) {
                    Text(
                        text = FocusSessionManager.formatRemainingTime(remainingSeconds),
                        color = PureWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = if (isSessionActive) {
                            "Bloqueo activo en perfil $activeProfile"
                        } else {
                            "Toca el botón central para encender el Modo Libre"
                        },
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de Temporizador Estricto y Programador Diario
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!canEnforceAppBlocking && restrictedPackages.isNotEmpty()) {
                                showPermissionsSheet = true
                            } else {
                                showStrictTimerDialog = true
                            }
                        },
                        enabled = !isSessionActive,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentIndigo,
                            contentColor = PureWhite,
                            disabledContainerColor = SurfaceZinc,
                            disabledContentColor = MutedGray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Temporizador Estricto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showScheduleDialog = true },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (scheduleEnabled) AccentEmerald else BorderZinc
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (scheduleEnabled) AccentEmerald.copy(alpha = 0.15f) else SurfaceZinc,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Programar",
                            tint = if (scheduleEnabled) AccentEmerald else PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // DOS CUADRADOS GRANDES SEPARADOS Y BIEN ESPACIADOS:
        // 1) Bloquear Apps (abre cuadrícula de 3 columnas instantánea)
        // 2) Silenciar Notificaciones (abre cuadrícula de 3 columnas instantánea)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SquareActionTile(
                modifier = Modifier.weight(1f),
                title = "Bloquear Apps",
                subtitle = "${restrictedPackages.size} bloqueadas",
                badgeText = if (restrictedPackages.isNotEmpty()) "ACTIVO" else "ABRIR",
                accentColor = AccentCoral,
                icon = Icons.Default.Lock,
                onClick = { activeGridPicker = AppGridPickerType.BLOCK_APPS }
            )

            SquareActionTile(
                modifier = Modifier.weight(1f),
                title = "Notificaciones",
                subtitle = "${mutedPackages.size} silenciadas",
                badgeText = if (mutedPackages.isNotEmpty()) "SILENCIO" else "ABRIR",
                accentColor = AccentViolet,
                icon = Icons.Default.NotificationsOff,
                onClick = { activeGridPicker = AppGridPickerType.MUTE_NOTIFICATIONS }
            )
        }

        // Tarjeta de Permisos del Sistema
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderZinc, RoundedCornerShape(18.dp))
                .clickable { showPermissionsSheet = true }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (canEnforceAppBlocking && hasNotificationPermission) AccentEmerald else AccentAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Permisos del sistema y bloqueo",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (canEnforceAppBlocking && hasNotificationPermission) {
                            "Todos los permisos activos correctamente"
                        } else {
                            "Toca para revisar o activar permisos en Android"
                        },
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "Configurar",
                color = AccentIndigo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Modal de Permisos Guiados
    if (showPermissionsSheet) {
        AlertDialog(
            onDismissRequest = { showPermissionsSheet = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = {
                Text("Permisos para Bloqueo Real", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Para que Android permita a Kanso cerrar y bloquear las aplicaciones elegidas, activa estos permisos:",
                        color = MutedGray,
                        fontSize = 12.sp
                    )

                    PermissionToggleRow(
                        title = "1. Mostrar sobre otras apps",
                        subtitle = "Permite mostrar la pantalla de bloqueo encima.",
                        isGranted = hasOverlayPermission,
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        }
                    )

                    PermissionToggleRow(
                        title = "2. Acceso de Uso (Detectar app)",
                        subtitle = "Detecta al instante cuando abres una app restringida.",
                        isGranted = hasUsageStatsPermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    PermissionToggleRow(
                        title = "3. Servicio de Accesibilidad",
                        subtitle = "Cierra automáticamente la app bloqueada.",
                        isGranted = hasAccessibilityPermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    PermissionToggleRow(
                        title = "4. Silenciador de Notificaciones",
                        subtitle = "Oculta avisos de las apps silenciadas.",
                        isGranted = hasNotificationPermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPermissionsSheet = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentIndigo,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo de Temporizador Estricto
    if (showStrictTimerDialog) {
        AlertDialog(
            onDismissRequest = { showStrictTimerDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = {
                Text("Temporizador Estricto (Sin salida)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Elige los minutos de concentración. No podrás apagar la sesión hasta que el contador llegue a 00:00.",
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
                            focusedBorderColor = AccentIndigo,
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
                        containerColor = AccentIndigo,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Encender bloqueo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStrictTimerDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // Diálogo de Programación Diaria
    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Programador diario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Inicia automáticamente una sesión de enfoque cada día a la hora elegida.",
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
                                focusedBorderColor = AccentIndigo,
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
                                focusedBorderColor = AccentIndigo,
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
                        containerColor = AccentIndigo,
                        contentColor = PureWhite
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
private fun SquareActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(152.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.2f),
                        SurfaceElevated
                    )
                )
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Column {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = MutedGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppGridPickerModal(
    pickerType: AppGridPickerType,
    installedApps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var searchQuery by remember { mutableStateOf("") }
    val isBlockingPicker = pickerType == AppGridPickerType.BLOCK_APPS
    val accentColor = if (isBlockingPicker) AccentCoral else AccentViolet

    val filteredApps = remember(installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderZinc, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBlockingPicker) "Bloquear Aplicaciones" else "Silenciar Notificaciones",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Toca un cuadrado para ${if (isBlockingPicker) "bloquear o desbloquear" else "silenciar o activar"}",
                        color = MutedGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${selectedPackages.size} activas",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = BorderZinc,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                cursorColor = accentColor
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Cuadrícula rápida de 3 columnas con cuadrados de icono + nombre abajo
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(
                items = filteredApps,
                key = { it.packageName },
                contentType = { "app_tile" }
            ) { app ->
                val isSelected = selectedPackages.contains(app.packageName)

                Box(
                    modifier = Modifier
                        .aspectRatio(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.22f)
                            else SurfaceElevated
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accentColor else BorderZinc,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onTogglePackage(app.packageName) }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else SurfaceZinc)
                            .border(1.dp, if (isSelected) accentColor else BorderZinc, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = if (isBlockingPicker) Icons.Default.Lock else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceZinc),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.label.take(2).uppercase(),
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = app.label,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceZinc)
            .border(
                1.dp,
                if (isGranted) AccentEmerald.copy(alpha = 0.6f) else BorderZinc,
                RoundedCornerShape(12.dp)
            )
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
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Activo",
                tint = AccentEmerald,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentIndigo)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Activar",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun profileGradientFor(profile: String): List<Color> {
    val gradients = listOf(
        listOf(AccentIndigo, AccentViolet),
        listOf(AccentEmerald, AccentCyan),
        listOf(AccentAmber, AccentCoral),
        listOf(AccentViolet, AccentCoral),
        listOf(AccentCyan, AccentIndigo)
    )
    return gradients[kotlin.math.abs(profile.hashCode()) % gradients.size]
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
