package com.kanso.focus.data

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kanso.focus.service.ScheduleReceiver
import com.kanso.focus.ui.BlockOverlayActivity
import com.kanso.focus.ui.InstalledAppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

object FocusSessionManager {
    private const val PREFS_NAME = "kanso_focus_prefs"
    private const val KEY_PROFILES = "profiles_list"
    private const val KEY_PROFILES_INITIALIZED = "profiles_initialized"
    private const val KEY_ACTIVE_PROFILE = "active_profile"
    private const val KEY_IS_ACTIVE = "is_session_active"
    private const val KEY_IS_STRICT = "is_strict_mode"
    private const val KEY_END_TIME = "end_time_millis"
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val KEY_SCHEDULE_HOUR = "schedule_hour"
    private const val KEY_SCHEDULE_MINUTE = "schedule_minute"
    private const val KEY_SCHEDULE_DURATION = "schedule_duration"
    private const val KEY_SCHEDULE_STRICT = "schedule_strict"

    private val defaultProfiles = listOf("Estudio", "Trabajo", "Descanso")

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var monitorJob: Job? = null
    private var preloadAppsJob: Job? = null
    private var lastOverlayLaunchMillis: Long = 0L

    private val profileAvatarBitmaps = ConcurrentHashMap<String, ImageBitmap>()

    private val _profiles = MutableStateFlow(defaultProfiles)
    val profiles: StateFlow<List<String>> = _profiles.asStateFlow()

    private val _profileImageVersion = MutableStateFlow(0)
    val profileImageVersion: StateFlow<Int> = _profileImageVersion.asStateFlow()

    private val _activeProfile = MutableStateFlow("Estudio")
    val activeProfile: StateFlow<String> = _activeProfile.asStateFlow()

    private val _restrictedPackages = MutableStateFlow<Set<String>>(emptySet())
    val restrictedPackages: StateFlow<Set<String>> = _restrictedPackages.asStateFlow()

    private val _mutedPackages = MutableStateFlow<Set<String>>(emptySet())
    val mutedPackages: StateFlow<Set<String>> = _mutedPackages.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _scheduleEnabled = MutableStateFlow(false)
    val scheduleEnabled: StateFlow<Boolean> = _scheduleEnabled.asStateFlow()

    private val _scheduleHour = MutableStateFlow(9)
    val scheduleHour: StateFlow<Int> = _scheduleHour.asStateFlow()

    private val _scheduleMinute = MutableStateFlow(0)
    val scheduleMinute: StateFlow<Int> = _scheduleMinute.asStateFlow()

    private val _scheduleDurationMinutes = MutableStateFlow(45)
    val scheduleDurationMinutes: StateFlow<Int> = _scheduleDurationMinutes.asStateFlow()

    val endTimeMillis: Long
        get() = prefs?.getLong(KEY_END_TIME, 0L) ?: 0L

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        if (prefs != null) {
            restoreSessionState()
            ensureForegroundMonitor()
            preloadInstalledAppsIfNeeded(appCtx)
            return
        }
        prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadProfiles()
        loadScheduleState()
        restoreSessionState()
        ensureForegroundMonitor()
        preloadInstalledAppsIfNeeded(appCtx)
        preloadProfileAvatars(appCtx)
    }

    private fun preloadInstalledAppsIfNeeded(context: Context) {
        if (_installedApps.value.isNotEmpty() || preloadAppsJob?.isActive == true) return
        preloadAppsJob = scope.launch(Dispatchers.IO) {
            val loaded = queryLaunchableAppsFast(context)
            _installedApps.value = loaded
        }
    }

    private fun preloadProfileAvatars(context: Context) {
        scope.launch(Dispatchers.IO) {
            _profiles.value.forEach { profile ->
                val path = prefs?.getString(profileImageKey(profile), null)
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.let { bmp ->
                            profileAvatarBitmaps[profile] = bmp.asImageBitmap()
                        }
                    }
                }
            }
            _profileImageVersion.value += 1
        }
    }

    fun getProfileAvatar(profile: String): ImageBitmap? {
        return profileAvatarBitmaps[profile]
    }

    fun setProfileImageFromUri(context: Context, profile: String, uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@launch

                val scaled = scaleCenterCropBitmap(decoded, 280)
                val avatarDir = File(context.filesDir, "profile_avatars").apply { mkdirs() }
                val safeFile = File(avatarDir, "avatar_${kotlin.math.abs(profile.hashCode())}.jpg")
                FileOutputStream(safeFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }

                prefs?.edit()?.putString(profileImageKey(profile), safeFile.absolutePath)?.apply()
                profileAvatarBitmaps[profile] = scaled.asImageBitmap()
                _profileImageVersion.value += 1
            } catch (_: Exception) {
            }
        }
    }

    private fun scaleCenterCropBitmap(src: Bitmap, targetSize: Int): Bitmap {
        val width = src.width.coerceAtLeast(1)
        val height = src.height.coerceAtLeast(1)
        val minDim = minOf(width, height)
        val cropX = (width - minDim) / 2
        val cropY = (height - minDim) / 2
        val square = Bitmap.createBitmap(src, cropX, cropY, minDim, minDim)
        return Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
    }

    private fun loadProfiles() {
        val initialized = prefs?.getBoolean(KEY_PROFILES_INITIALIZED, false) ?: false
        val savedProfiles = prefs?.getStringSet(KEY_PROFILES, null)?.toList()
        if (!initialized || savedProfiles.isNullOrEmpty()) {
            _profiles.value = defaultProfiles
            prefs?.edit()
                ?.putStringSet(KEY_PROFILES, defaultProfiles.toSet())
                ?.putBoolean(KEY_PROFILES_INITIALIZED, true)
                ?.apply()
        } else {
            val ordered = defaultProfiles.filter { savedProfiles.contains(it) } +
                    savedProfiles.filter { !defaultProfiles.contains(it) }.sorted()
            _profiles.value = ordered
        }

        val current = prefs?.getString(KEY_ACTIVE_PROFILE, _profiles.value.first()) ?: _profiles.value.first()
        _activeProfile.value = if (_profiles.value.contains(current)) current else _profiles.value.first()
        loadProfilePackages(_activeProfile.value)
    }

    private fun loadProfilePackages(profile: String) {
        val restricted = prefs?.getStringSet(restrictedKey(profile), emptySet()) ?: emptySet()
        val muted = prefs?.getStringSet(mutedKey(profile), emptySet()) ?: emptySet()
        _restrictedPackages.value = restricted.toSet()
        _mutedPackages.value = muted.toSet()
    }

    private fun restrictedKey(profile: String) = "restricted_$profile"
    private fun mutedKey(profile: String) = "muted_$profile"
    private fun profileImageKey(profile: String) = "profile_img_$profile"

    fun selectProfile(profile: String) {
        if (!_profiles.value.contains(profile)) return
        _activeProfile.value = profile
        prefs?.edit()?.putString(KEY_ACTIVE_PROFILE, profile)?.apply()
        loadProfilePackages(profile)
    }

    fun addProfile(name: String, context: Context? = null, imageUri: Uri? = null) {
        val clean = name.trim()
        if (clean.isEmpty() || _profiles.value.contains(clean)) return
        val updated = _profiles.value + clean
        _profiles.value = updated
        prefs?.edit()?.putStringSet(KEY_PROFILES, updated.toSet())?.apply()
        selectProfile(clean)
        if (context != null && imageUri != null) {
            setProfileImageFromUri(context, clean, imageUri)
        }
    }

    fun deleteProfile(name: String): Boolean {
        if (_profiles.value.size <= 1) return false
        val updated = _profiles.value.filterNot { it == name }
        if (updated.isEmpty()) return false
        _profiles.value = updated

        val oldImgPath = prefs?.getString(profileImageKey(name), null)
        if (!oldImgPath.isNullOrBlank()) {
            runCatching { File(oldImgPath).delete() }
        }
        profileAvatarBitmaps.remove(name)

        prefs?.edit()
            ?.putStringSet(KEY_PROFILES, updated.toSet())
            ?.remove(restrictedKey(name))
            ?.remove(mutedKey(name))
            ?.remove(profileImageKey(name))
            ?.apply()
        if (_activeProfile.value == name) {
            selectProfile(updated.first())
        }
        _profileImageVersion.value += 1
        return true
    }

    fun toggleRestrictedPackage(packageName: String) {
        val current = _restrictedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _restrictedPackages.value = current
        prefs?.edit()?.putStringSet(restrictedKey(_activeProfile.value), current)?.apply()
    }

    fun toggleMutedPackage(packageName: String) {
        val current = _mutedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _mutedPackages.value = current
        prefs?.edit()?.putStringSet(mutedKey(_activeProfile.value), current)?.apply()
    }

    fun startStrictTimedSession(minutes: Int) {
        val validMinutes = minutes.coerceAtLeast(1)
        val endMillis = System.currentTimeMillis() + validMinutes * 60_000L
        _isStrictMode.value = true
        _isSessionActive.value = true
        prefs?.edit()
            ?.putBoolean(KEY_IS_ACTIVE, true)
            ?.putBoolean(KEY_IS_STRICT, true)
            ?.putLong(KEY_END_TIME, endMillis)
            ?.apply()
        startCountdownLoop(endMillis)
        ensureForegroundMonitor()
    }

    fun startFlexibleSession(minutes: Int = 0) {
        val endMillis = if (minutes > 0) {
            System.currentTimeMillis() + minutes * 60_000L
        } else {
            0L
        }
        _isStrictMode.value = false
        _isSessionActive.value = true
        prefs?.edit()
            ?.putBoolean(KEY_IS_ACTIVE, true)
            ?.putBoolean(KEY_IS_STRICT, false)
            ?.putLong(KEY_END_TIME, endMillis)
            ?.apply()
        if (endMillis > 0L) {
            startCountdownLoop(endMillis)
        } else {
            timerJob?.cancel()
            _remainingSeconds.value = 0L
        }
        ensureForegroundMonitor()
    }

    fun stopSession(): Boolean {
        val strict = _isStrictMode.value
        val endMillis = endTimeMillis
        val now = System.currentTimeMillis()
        if (strict && endMillis > now) {
            return false
        }
        clearSessionState()
        return true
    }

    private fun clearSessionState() {
        timerJob?.cancel()
        monitorJob?.cancel()
        _isSessionActive.value = false
        _isStrictMode.value = false
        _remainingSeconds.value = 0L
        prefs?.edit()
            ?.putBoolean(KEY_IS_ACTIVE, false)
            ?.putBoolean(KEY_IS_STRICT, false)
            ?.putLong(KEY_END_TIME, 0L)
            ?.apply()
    }

    private fun restoreSessionState() {
        val active = prefs?.getBoolean(KEY_IS_ACTIVE, false) ?: false
        val strict = prefs?.getBoolean(KEY_IS_STRICT, false) ?: false
        val endMillis = prefs?.getLong(KEY_END_TIME, 0L) ?: 0L
        val now = System.currentTimeMillis()

        if (!active) {
            _isSessionActive.value = false
            _isStrictMode.value = false
            _remainingSeconds.value = 0L
            return
        }

        if (endMillis > 0L) {
            if (endMillis > now) {
                _isSessionActive.value = true
                _isStrictMode.value = strict
                startCountdownLoop(endMillis)
            } else {
                clearSessionState()
            }
        } else {
            _isSessionActive.value = true
            _isStrictMode.value = false
            _remainingSeconds.value = 0L
        }
    }

    private fun startCountdownLoop(endMillis: Long) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val diffMillis = endMillis - System.currentTimeMillis()
                if (diffMillis <= 0L) {
                    clearSessionState()
                    break
                }
                val nextSecs = (diffMillis + 999L) / 1000L
                if (_remainingSeconds.value != nextSecs) {
                    _remainingSeconds.value = nextSecs
                }
                delay(1000L)
            }
        }
    }

    private fun ensureForegroundMonitor() {
        if (!_isSessionActive.value) {
            monitorJob?.cancel()
            return
        }
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch(Dispatchers.Default) {
            while (_isSessionActive.value) {
                val ctx = appContext
                if (ctx != null && _restrictedPackages.value.isNotEmpty() && hasUsageStatsPermission(ctx)) {
                    val topPkg = getForegroundPackage(ctx)
                    if (topPkg != null &&
                        topPkg != ctx.packageName &&
                        _restrictedPackages.value.contains(topPkg)
                    ) {
                        triggerBlockOverlay(ctx, topPkg)
                    }
                }
                delay(450L)
            }
        }
    }

    fun triggerBlockOverlay(context: Context, blockedPkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastOverlayLaunchMillis < 500L) return
        lastOverlayLaunchMillis = now

        val overlayIntent = Intent(context.applicationContext, BlockOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("blocked_package", blockedPkg)
        }
        context.applicationContext.startActivity(overlayIntent)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun getForegroundPackage(context: Context): String? {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 10_000L
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            var lastForegroundPkg: String? = null
            var lastTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    if (event.timeStamp >= lastTime) {
                        lastTime = event.timeStamp
                        lastForegroundPkg = event.packageName
                    }
                }
            }
            lastForegroundPkg
        } catch (_: Exception) {
            null
        }
    }

    private fun loadScheduleState() {
        _scheduleEnabled.value = prefs?.getBoolean(KEY_SCHEDULE_ENABLED, false) ?: false
        _scheduleHour.value = prefs?.getInt(KEY_SCHEDULE_HOUR, 9) ?: 9
        _scheduleMinute.value = prefs?.getInt(KEY_SCHEDULE_MINUTE, 0) ?: 0
        _scheduleDurationMinutes.value = prefs?.getInt(KEY_SCHEDULE_DURATION, 45) ?: 45
    }

    fun scheduleDailySession(
        context: Context,
        hourOfDay: Int,
        minute: Int,
        durationMinutes: Int = 45,
        strictMode: Boolean = true
    ) {
        init(context)
        _scheduleEnabled.value = true
        _scheduleHour.value = hourOfDay
        _scheduleMinute.value = minute
        _scheduleDurationMinutes.value = durationMinutes

        prefs?.edit()
            ?.putBoolean(KEY_SCHEDULE_ENABLED, true)
            ?.putInt(KEY_SCHEDULE_HOUR, hourOfDay)
            ?.putInt(KEY_SCHEDULE_MINUTE, minute)
            ?.putInt(KEY_SCHEDULE_DURATION, durationMinutes)
            ?.putBoolean(KEY_SCHEDULE_STRICT, strictMode)
            ?.apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            putExtra("duration_minutes", durationMinutes)
            putExtra("strict_mode", strictMode)
            putExtra("hour", hourOfDay)
            putExtra("minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailySchedule(context: Context) {
        init(context)
        _scheduleEnabled.value = false
        prefs?.edit()?.putBoolean(KEY_SCHEDULE_ENABLED, false)?.apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun formatRemainingTime(seconds: Long): String {
        if (seconds <= 0L) return "00:00"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    private fun drawableToCompactImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            val targetPx = 96
            val bitmap = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, targetPx, targetPx)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun queryLaunchableAppsFast(context: Context): List<InstalledAppInfo> =
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
                .asSequence()
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == context.packageName) return@mapNotNull null
                    pkg to info
                }
                .distinctBy { it.first }
                .map { (pkg, info) ->
                    val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                    val iconBitmap = drawableToCompactImageBitmap(info.loadIcon(pm))
                    InstalledAppInfo(
                        label = label,
                        packageName = pkg,
                        iconBitmap = iconBitmap
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
}
