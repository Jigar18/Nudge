package app.nudge.reminders

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.nudge.reminders.alarm.ReminderReceiver
import app.nudge.reminders.ui.HomeScreen
import app.nudge.reminders.ui.HomeViewModel
import app.nudge.reminders.ui.theme.NudgeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(320)
                .withEndAction { provider.remove() }
                .start()
        }
        enableEdgeToEdge()
        focusTaskFrom(intent)
        setContent {
            NudgeTheme {
                HomeScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focusTaskFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        // Covers alarms lost to a force-stop and permissions granted while we were in Settings.
        viewModel.sync()
    }

    private fun focusTaskFrom(intent: Intent) {
        val taskId = intent.getLongExtra(ReminderReceiver.EXTRA_TASK_ID, -1)
        if (taskId > 0) viewModel.focus(taskId)
    }
}
