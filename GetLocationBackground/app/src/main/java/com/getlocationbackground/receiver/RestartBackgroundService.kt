package com.getlocationbackground.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.getlocationbackground.service.LocationService

class RestartBackgroundService : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("location", "Service tried to stop")
        context?.let {
            val newIntent = Intent(it, LocationService::class.java)
                Log.i("location","Service restarted")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.startForegroundService(newIntent)
                } else {
                    it.startService(Intent(it, LocationService::class.java))
                }
        }?:run {
            Log.i("location", "context null")
        }
    }
}