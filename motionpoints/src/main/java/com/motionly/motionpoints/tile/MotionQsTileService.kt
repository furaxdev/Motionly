package com.motionly.motionpoints.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.motionly.motionpoints.R
import com.motionly.motionpoints.overlay.MotionOverlayService
import com.motionly.motionpoints.overlay.ProjectionRequestActivity

/** Quick Settings panel toggle for the motion cues overlay. */
class MotionQsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (MotionOverlayService.isRunning) {
            stopService(Intent(this, MotionOverlayService::class.java))
            refreshTile()
        } else {
            val intent = Intent(this, ProjectionRequestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun refreshTile() {
        qsTile?.apply {
            state = if (MotionOverlayService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            updateTile()
        }
    }
}
