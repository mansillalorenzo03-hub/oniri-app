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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
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
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kanso.focus.ui.theme.OpticalYellow
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
import com.kanso.focus.ui.theme.SurfaceElevated
import com.kanso.focus.ui.theme.SurfaceZinc
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val iconBitmap: ImageBitmap?,
    val isSuggestedDistraction: Boolean = false
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

@OptIn(ExperimentalFoundationApi::class)
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

    // Diálogo de administración de un perfil individual (desde modo Administrar o Long-Click)
    var profileBeingEdited by remember { mutableStateOf<String?>(null) }

    val existingProfilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val targetProfile = profileBeingEdited
        if (uri != null && targetProfile != null) {
            FocusSessionManager.setProfileImageFromUri(context, targetProfile, uri)
        }
        profileBeingEdited = null
    }

    val newProfilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            newProfileImageUri = uri
        }
    }

    Scaffold(
        containerColor = PureBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo K blanca sobre fondo negro
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.5.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    color = PureWhite,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "KANSO",
                color = PureWhite,
                fontSize = 20.sp,
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
                    "Toca cualquier perfil para cambiar su foto o eliminarlo"
                } else {
                    "Toca un perfil para entrar o mantén presionado para editarlo"
                },
                color = MutedGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

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
                    val avatarBitmap = remember(profile, profileImageVersion) {
                        FocusSessionManager.getProfileAvatar(profile)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Tarjeta limpia sin botón flotante de cámara superpuesto.
                        // Si no tiene foto, renderiza un icono vectorial ilustrativo limpio.
                        Box(
                            modifier = Modifier
                                .size(118.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Brush.linearGradient(profileGradientFor(profile)))
                                .border(
                                    width = 2.dp,
                                    color = if (isManageMode) OpticalYellow else PureWhite.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (isManageMode) {
                                            profileBeingEdited = profile
                                        } else {
                                            onProfilePicked(profile)
                                        }
                                    },
                                    onLongClick = {
                                        profileBeingEdited = profile
                                    }
                                ),
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
                                Icon(
                                    imageVector = illustrativeIconForProfile(profile),
                                    contentDescription = profile,
                                    tint = PureWhite,
                                    modifier = Modifier.size(50.dp)
                                )
                            }

                            if (isManageMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PureBlack.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(OpticalYellow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar perfil",
                                            tint = PureBlack,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
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
                                .size(118.dp)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Añadir perfil",
                            color = MutedGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { isManageMode = !isManageMode },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isManageMode) OpticalYellow else BorderZinc),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isManageMode) OpticalYellow.copy(alpha = 0.16f) else SurfaceElevated,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = if (isManageMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isManageMode) OpticalYellow else PureWhite,
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

    // Diálogo para Crear Perfil Nuevo
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
                            focusedBorderColor = OpticalYellow,
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
                            text = if (newProfileImageUri != null) "Imagen seleccionada ✓" else "Elegir foto de galería (opcional)",
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
                        containerColor = OpticalYellow,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Crear perfil", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
    }

    // Diálogo de edición/eliminación de perfil individual (por modo Administrar o Long-Click)
    if (profileBeingEdited != null) {
        val target = profileBeingEdited!!
        val canDelete = profiles.size > 1

        AlertDialog(
            onDismissRequest = { profileBeingEdited = null },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Editar perfil \"$target\"", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Elige una acción para este perfil:",
                        color = MutedGray,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = {
                            existingProfilePhotoLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticalYellow),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceZinc,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = OpticalYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cambiar foto de galería", fontWeight = FontWeight.Bold)
                    }

                    if (canDelete) {
                        OutlinedButton(
                            onClick = {
                                FocusSessionManager.deleteProfile(target)
                                profileBeingEdited = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentCoral),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AccentCoral.copy(alpha = 0.14f),
                                contentColor = AccentCoral
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = AccentCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar perfil", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { profileBeingEdited = null }) {
                    Text("Cerrar", color = PureWhite, fontWeight = FontWeight.Bold)
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
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
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
                                    text = "Menú principal",
                                    color = MutedGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

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
                                        Icon(
                                            imageVector = illustrativeIconForProfile(activeProfile),
                                            contentDescription = activeProfile,
                                            tint = PureWhite,
                                            modifier = Modifier.size(24.dp)
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
                                        text = "Cambiar perfil",
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Cambiar perfil",
                                tint = OpticalYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = BorderZinc)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "FUNCIONES",
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
                                selectedContainerColor = OpticalYellow.copy(alpha = 0.18f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = OpticalYellow,
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
                                        text = "Dislexia, Poca Visión y 5 Voces HD",
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
                                selectedContainerColor = OpticalYellow.copy(alpha = 0.18f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = OpticalYellow,
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
        Scaffold(
            containerColor = PureBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
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
                                Icon(
                                    imageVector = illustrativeIconForProfile(activeProfile),
                                    contentDescription = activeProfile,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeProfile,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedSection == 0) {
                    FocusModeDashboard()
                } else {
                    AdaptiveReaderScreen()
                }
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

    // Selección de pestaña de modo de trabajo: false = [ Modo Libre ], true = [ Con Temporizador ]
    var selectedTimedTab by remember(isStrictMode) { mutableStateOf(isStrictMode) }
    var selectedDurationMinutes by remember { mutableIntStateOf(25) }

    var activeGridPicker by remember { mutableStateOf(AppGridPickerType.NONE) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("25") }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleHourInput by remember { mutableStateOf(scheduleHour.toString()) }
    var scheduleMinuteInput by remember { mutableStateOf(String.format("%02d", scheduleMinute)) }

    val canEnforceAppBlocking = hasAccessibilityPermission || (hasUsageStatsPermission && hasOverlayPermission)
    val totalInstalledCount = installedApps.size.coerceAtLeast(1)

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
            onSelectSuggested = {
                FocusSessionManager.selectSuggestedPackages(
                    forBlocking = activeGridPicker == AppGridPickerType.BLOCK_APPS
                )
            },
            onClearAll = {
                FocusSessionManager.clearAllSelectedPackages(
                    forBlocking = activeGridPicker == AppGridPickerType.BLOCK_APPS
                )
            },
            onClose = { activeGridPicker = AppGridPickerType.NONE }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner de permisos si falta alguno
        if (!canEnforceAppBlocking || !hasNotificationPermission) {
            Card(
                onClick = { showPermissionsSheet = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Activar permisos de bloqueo",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toca para habilitar el cierre de apps y notificaciones",
                                color = MutedGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = AccentAmber,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Activar",
                            color = PureBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Tarjeta Principal de Modo Enfoque (Sin Switch redundante; con pestañas [ Modo Libre ] | [ Con Temporizador ])
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceZinc),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isSessionActive) OpticalYellow else BorderZinc
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pestañas superiores (Chips) para elegir entre [ Modo Libre ] y [ Con Temporizador ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !selectedTimedTab,
                        onClick = { if (!isSessionActive) selectedTimedTab = false },
                        enabled = !isSessionActive || !selectedTimedTab,
                        label = {
                            Text(
                                text = "Modo Libre",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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

                    FilterChip(
                        selected = selectedTimedTab,
                        onClick = { if (!isSessionActive) selectedTimedTab = true },
                        enabled = !isSessionActive || selectedTimedTab,
                        label = {
                            Text(
                                text = "Con Temporizador",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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

                // Si está en pestaña [ Con Temporizador ] y aún no inició, muestra chips de minutos
                if (selectedTimedTab && !isSessionActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val isSelectedMin = selectedDurationMinutes == mins
                            FilterChip(
                                selected = isSelectedMin,
                                onClick = { selectedDurationMinutes = mins },
                                label = {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentIndigo,
                                    selectedLabelColor = PureWhite,
                                    containerColor = SurfaceElevated,
                                    labelColor = MutedGray
                                )
                            )
                        }
                        FilterChip(
                            selected = selectedDurationMinutes !in listOf(15, 25, 45, 60),
                            onClick = {
                                customMinutesInput = selectedDurationMinutes.toString()
                                showCustomMinutesDialog = true
                            },
                            label = {
                                Text(
                                    text = "Otro",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentIndigo,
                                selectedLabelColor = PureWhite,
                                containerColor = SurfaceElevated,
                                labelColor = MutedGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ÚNICO Botón Central Circular Tipo Power Interactivo
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSessionActive) {
                                Brush.radialGradient(
                                    listOf(OpticalYellow, OpticalYellow.copy(alpha = 0.25f))
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(SurfaceElevated, PureBlack)
                                )
                            }
                        )
                        .border(
                            width = 3.dp,
                            color = if (isSessionActive) OpticalYellow else AccentIndigo,
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
                                } else if (selectedTimedTab) {
                                    FocusSessionManager.startStrictTimedSession(selectedDurationMinutes)
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
                            tint = if (isSessionActive) PureBlack else PureWhite,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSessionActive) "ACTIVO" else "INICIAR",
                            color = if (isSessionActive) PureBlack else PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isSessionActive && remainingSeconds > 0L) {
                    Text(
                        text = FocusSessionManager.formatRemainingTime(remainingSeconds),
                        color = OpticalYellow,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modo estricto activo · Sin salida hasta 00:00",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = when {
                            isSessionActive -> "Modo libre encendido · Toca el botón central para apagar"
                            selectedTimedTab -> "Temporizador estricto de $selectedDurationMinutes min (Sin salida)"
                            else -> "Toca el botón central para encender el Modo Libre"
                        },
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón secundario de Programación Diaria
                OutlinedButton(
                    onClick = { showScheduleDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (scheduleEnabled) AccentEmerald else BorderZinc
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (scheduleEnabled) AccentEmerald.copy(alpha = 0.14f) else SurfaceElevated,
                        contentColor = PureWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (scheduleEnabled) AccentEmerald else PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scheduleEnabled) {
                            "Programado diario a las ${String.format("%02d:%02d", scheduleHour, scheduleMinute)}"
                        } else {
                            "Programar horario diario automático"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tarjetas inferiores clickeables en toda su superficie con ripple y resumen "X de Y apps" (sin botón "ABRIR")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryActionCard(
                modifier = Modifier.weight(1f),
                title = "Bloquear Apps",
                summaryText = "${restrictedPackages.size} de $totalInstalledCount apps",
                accentColor = AccentCoral,
                icon = Icons.Default.Lock,
                onClick = { activeGridPicker = AppGridPickerType.BLOCK_APPS }
            )

            SummaryActionCard(
                modifier = Modifier.weight(1f),
                title = "Notificaciones",
                summaryText = "${mutedPackages.size} de $totalInstalledCount apps",
                accentColor = AccentViolet,
                icon = Icons.Default.NotificationsOff,
                onClick = { activeGridPicker = AppGridPickerType.MUTE_NOTIFICATIONS }
            )
        }

        // Tarjeta de Permisos del Sistema protegida con navigationBarsPadding()
        Card(
            onClick = { showPermissionsSheet = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceZinc),
            border = BorderStroke(1.dp, BorderZinc),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        tint = if (canEnforceAppBlocking && hasNotificationPermission) AccentEmerald else OpticalYellow,
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
                        Text(
                            text = if (canEnforceAppBlocking && hasNotificationPermission) {
                                "Todos los permisos activos correctamente"
                            } else {
                                "Revisar o activar permisos en Android"
                            },
                            color = MutedGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Configurar",
                    color = OpticalYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Diálogo para personalizar minutos del temporizador
    if (showCustomMinutesDialog) {
        AlertDialog(
            onDismissRequest = { showCustomMinutesDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = { Text("Minutos del temporizador", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = { customMinutesInput = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Minutos", color = MutedGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpticalYellow,
                        unfocusedBorderColor = BorderZinc,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDurationMinutes = customMinutesInput.toIntOrNull()?.coerceAtLeast(1) ?: 25
                        showCustomMinutesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpticalYellow,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutesDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            }
        )
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
                        text = "Activa estos permisos nativos para garantizar el bloqueo inmediato:",
                        color = MutedGray,
                        fontSize = 12.sp
                    )

                    PermissionToggleRow(
                        title = "1. Mostrar sobre otras apps",
                        subtitle = "Permite superponer la pantalla de bloqueo.",
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
                        containerColor = OpticalYellow,
                        contentColor = PureBlack
                    )
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
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
                                focusedBorderColor = OpticalYellow,
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
                                focusedBorderColor = OpticalYellow,
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
                        containerColor = OpticalYellow,
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
private fun SummaryActionCard(
    modifier: Modifier = Modifier,
    title: String,
    summaryText: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Card de Material 3 con ripple nativo en toda la superficie y sin botón de texto interno "ABRIR"
    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceZinc),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summaryText,
                    color = MutedGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppGridPickerModal(
    pickerType: AppGridPickerType,
    installedApps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onSelectSuggested: () -> Unit,
    onClearAll: () -> Unit,
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
            .navigationBarsPadding()
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
                        text = "${selectedPackages.size} de ${installedApps.size} seleccionadas",
                        color = OpticalYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                focusedContainerColor = SurfaceZinc,
                unfocusedContainerColor = SurfaceZinc,
                focusedBorderColor = OpticalYellow,
                unfocusedBorderColor = BorderZinc,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                cursorColor = OpticalYellow
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Barra superior de acción rápida: "Bloquear sugeridas" y "Desmarcar todas"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSelectSuggested,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OpticalYellow.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = OpticalYellow
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bloquear sugeridas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderZinc),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceZinc,
                    contentColor = MutedGray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Desmarcar todas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cuadrícula de 3 columnas con Checkbox nativo Material 3 de alto contraste y nombres simétricos a 1 línea
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(
                items = filteredApps,
                key = { it.packageName },
                contentType = { "app_tile" }
            ) { app ->
                val isSelected = selectedPackages.contains(app.packageName)

                Card(
                    onClick = { onTogglePackage(app.packageName) },
                    modifier = Modifier.aspectRatio(0.88f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SurfaceElevated else SurfaceZinc
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) OpticalYellow else BorderZinc
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        // Checkbox nativo de Material 3 con alto contraste en la esquina superior derecha
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = OpticalYellow,
                                uncheckedColor = MutedGray,
                                checkmarkColor = PureBlack
                            ),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = app.label,
                                color = if (isSelected) OpticalYellow else PureWhite,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                    .background(OpticalYellow)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Activar",
                    color = PureBlack,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

private fun illustrativeIconForProfile(profile: String): ImageVector {
    return when (profile.trim().lowercase()) {
        "estudio" -> Icons.Default.MenuBook
        "trabajo" -> Icons.Default.WorkOutline
        "descanso" -> Icons.Default.SelfImprovement
        else -> Icons.Default.AutoAwesome
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
