package dev.todobar.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Settings / onboarding surface. Drives the two permissions the floating
 * bubble needs — overlay + post-notifications — and starts/stops the bubble
 * service.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var overlayCard: View
    private lateinit var notifCard: View
    private lateinit var overlayStatus: TextView
    private lateinit var notifStatus: TextView
    private lateinit var overlayButton: Button
    private lateinit var notifButton: Button
    private lateinit var bubbleStatus: TextView
    private lateinit var bubbleButton: Button
    private lateinit var bubbleIcon: ImageView

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayCard = findViewById(R.id.overlay_card)
        notifCard = findViewById(R.id.notif_card)
        overlayStatus = findViewById(R.id.overlay_status)
        notifStatus = findViewById(R.id.notif_status)
        overlayButton = findViewById(R.id.overlay_button)
        notifButton = findViewById(R.id.notif_button)
        bubbleStatus = findViewById(R.id.bubble_status)
        bubbleButton = findViewById(R.id.bubble_button)
        bubbleIcon = findViewById(R.id.bubble_icon)

        overlayButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            startActivity(intent)
        }

        notifButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        bubbleButton.setOnClickListener {
            if (isBubbleRunning) {
                BubbleService.stop(this)
            } else {
                if (!Settings.canDrawOverlays(this)) return@setOnClickListener
                BubbleService.start(this)
            }
            refreshState()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val hasOverlay = Settings.canDrawOverlays(this)
        overlayStatus.text = getString(
            if (hasOverlay) R.string.permission_granted else R.string.permission_needed,
        )
        overlayButton.text = getString(
            if (hasOverlay) R.string.action_reopen_settings else R.string.action_grant,
        )

        val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val hasNotif = !needsNotif || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        notifStatus.text = getString(
            if (hasNotif) R.string.permission_granted else R.string.permission_needed,
        )
        notifCard.visibility = if (needsNotif) View.VISIBLE else View.GONE
        notifButton.isEnabled = needsNotif && !hasNotif

        val running = isBubbleRunning
        bubbleStatus.text = getString(
            if (running) R.string.bubble_running else R.string.bubble_idle,
        )
        bubbleButton.text = getString(
            if (running) R.string.action_stop_bubble else R.string.action_start_bubble,
        )
        bubbleButton.isEnabled = hasOverlay
        bubbleIcon.alpha = if (running) 1f else 0.6f
    }

    private val isBubbleRunning: Boolean
        get() = BubbleService.isRunning && Settings.canDrawOverlays(this)
}
