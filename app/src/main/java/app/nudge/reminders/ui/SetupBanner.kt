package app.nudge.reminders.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.nudge.reminders.NudgeApp

private enum class SetupIssue(val icon: ImageVector, val title: String, val message: String, val dismissible: Boolean) {
    NOTIFICATIONS(Icons.Rounded.NotificationsOff, "Notifications are off", "Nudge can't alert you or pin reminders without them.", false),
    EXACT_ALARMS(Icons.Rounded.AlarmOn, "Allow alarms & reminders", "Needed so reminders ring at the exact minute.", false),
    BATTERY(Icons.Rounded.BatteryChargingFull, "Keep reminders on time", "Let Nudge run in the background so your phone's battery saver can't delay them.", true),
}

/** Surfaces the most important missing permission, re-checking whenever the app returns to the foreground. */
@Composable
fun SetupBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge", Context.MODE_PRIVATE) }
    var refresh by remember { mutableIntStateOf(0) }
    var batteryDismissed by remember { mutableStateOf(prefs.getBoolean(KEY_BATTERY_DISMISSED, false)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    LaunchedEffect(Unit) {
        if (needsNotificationPermission(context) && !prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) {
            prefs.edit { putBoolean(KEY_ASKED_NOTIFICATIONS, true) }
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val issue = remember(refresh, batteryDismissed) {
        val reminders = (context.applicationContext as NudgeApp).reminders
        val power = context.getSystemService(PowerManager::class.java)
        when {
            !reminders.notifier.canPost() -> SetupIssue.NOTIFICATIONS
            !reminders.scheduler.canScheduleExact() -> SetupIssue.EXACT_ALARMS
            !batteryDismissed && !power.isIgnoringBatteryOptimizations(context.packageName) -> SetupIssue.BATTERY
            else -> null
        }
    }

    AnimatedVisibility(
        visible = issue != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val shown = issue ?: return@AnimatedVisibility
        val colors = MaterialTheme.colorScheme
        val urgent = shown != SetupIssue.BATTERY
        val accent = if (urgent) colors.secondary else colors.primary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = accent.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        ) {
            Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(shown.icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shown.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Text(shown.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                TextButton(onClick = {
                    when (shown) {
                        SetupIssue.NOTIFICATIONS -> context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                        SetupIssue.EXACT_ALARMS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }
                        SetupIssue.BATTERY -> context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                        )
                    }
                }) {
                    Text(if (shown == SetupIssue.NOTIFICATIONS) "Turn on" else "Allow", color = accent)
                }
                if (shown.dismissible) {
                    IconButton(onClick = {
                        prefs.edit { putBoolean(KEY_BATTERY_DISMISSED, true) }
                        batteryDismissed = true
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun needsNotificationPermission(context: Context) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
private const val KEY_BATTERY_DISMISSED = "battery_banner_dismissed"
