package com.motionly.motionpoints.overlay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Invisible trampoline that walks the user through granting the overlay permission and
 * the one-time screen-capture consent, then hands the result to [MotionOverlayService].
 */
class ProjectionRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        @Suppress("DEPRECATION")
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, MotionOverlayService::class.java)
                .putExtra(MotionOverlayService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(MotionOverlayService.EXTRA_RESULT_DATA, data)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
        finish()
    }

    companion object {
        private const val REQUEST_CAPTURE = 1001
    }
}
