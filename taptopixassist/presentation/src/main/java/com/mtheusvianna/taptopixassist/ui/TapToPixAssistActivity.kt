package com.mtheusvianna.taptopixassist.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mtheusvianna.taptopixassist.presentation.R
import com.mtheusvianna.taptopixassist.presentation.databinding.ActivityTapToPixAssistBinding
import com.mtheusvianna.taptopixassist.service.TapToPixService
import com.mtheusvianna.taptopixassist.ui.dashboard.DashboardViewModel
import com.mtheusvianna.taptopixassist.ui.notifications.NotificationsViewModel

class TapToPixAssistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTapToPixAssistBinding

    val dashboardViewModel: DashboardViewModel by viewModels()
    val notificationsViewModel: NotificationsViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTapToPixAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setUpNav()
    }

    override fun onResume() {
        super.onResume()
        notificationsViewModel.updateNfcStatusWith(isAvailable = nfcAdapter?.isEnabled == true)
        registerReceiver(
            tapToPixReceivedBroadcastReceiver,
            IntentFilter(getString(R.string.action_tap_to_pix_received)),
            RECEIVER_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(tapToPixReceivedBroadcastReceiver)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isActionTagDiscovered = intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
        if (isActionTagDiscovered && currentDestinationIdIsEqualTo(R.id.navigation_dashboard)) {
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            tag?.let { dashboardViewModel.emitDiscovered(tag) }
        }
    }

    private fun findNavController(): NavController = findNavController(R.id.nav_host_fragment_activity_main)

    private fun currentDestinationIdIsEqualTo(@IdRes id: Int) = findNavController().currentDestination?.id == id

    private fun setUpNav() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                when (destination.id) {
                    R.id.navigation_notifications -> {
                        val serviceComponent = ComponentName(packageName, TapToPixService::class.java.name)
                        CardEmulation.getInstance(nfcAdapter)
                            .setPreferredService(this, serviceComponent)
                    }
                    // can't unset on fragment since the activity's onPause is called before the fragment's onPause
                    else -> CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this)
                }
            }
        }
    }

    private val tapToPixReceivedBroadcastReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.VIBRATE)
        override fun onReceive(context: Context?, intent: Intent?) {
            val isActionTapToPixReceived = intent?.action == getString(R.string.action_tap_to_pix_received)
            if (isActionTapToPixReceived) {
                val value = intent.getStringExtra(getString(R.string.extra_tap_to_pix_uri))
                if (currentDestinationIdIsEqualTo(R.id.navigation_notifications)) {
                    value?.let { notificationsViewModel.updateTextWith(value) }
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(500, 255))
                }
            }
        }
    }
}
