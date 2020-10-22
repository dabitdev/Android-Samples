package com.getlocationbackground.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.getlocationbackground.R
import com.getlocationbackground.receiver.RestartBackgroundService
import com.google.android.gms.location.*

class LocationService : Service() {
    var counter = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    lateinit var client : FusedLocationProviderClient
    var callback:LocationCallback = LocationUpdate()
    override fun onCreate() {
        super.onCreate()
        Log.i("location", "onCreate")
        client = LocationServices.getFusedLocationProviderClient(applicationContext)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            createNotificationChanel()
        else startForeground(1, Notification())
        requestLocationUpdates()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChanel() {
        val NOTIFICATION_CHANNEL_ID = "com.getlocationbackground"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running count::$counter")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if(intent == null) {
            Log.i("location", "intent = null")

        } else {
            Log.i("location", intent.toString())
        }

        return START_STICKY_COMPATIBILITY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("location", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
        cleanAndRestart()

    }
    override fun onDestroy() {
        super.onDestroy()
        Log.i("location", "onDestroy tried to stop")
        cleanAndRestart()
    }

    private fun cleanAndRestart(){
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, RestartBackgroundService::class.java)

        val voidTask = client.removeLocationUpdates(callback)

        voidTask.addOnCompleteListener {
            stopForeground(true)
            this.sendBroadcast(broadcastIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 10000
        request.fastestInterval = 5000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, callback, null)
        }
    }

    inner class LocationUpdate:LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                counter++
                latitude = it.latitude
                longitude = it.longitude
                Log.d("location", "$this $counter location update $it")
            }
        }
    }
}

// CASES
// CASE 1: user kills the app from app manager, it will trigger onTaskRemoved() and easy to restart service
// CASE 2: if you do not have the flag    android:stopWithTask="true"
// onDestroy sometimes is triggered, and sometimes the service is able to restart otherwise services stops onCreate

// BONUS(CASE STOP BUTTON):maybe is worth checking if user presses STOP, because it will never trigger onDestroy or Stop
// Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
//                    Uri.fromParts("package", mAppEntry.info.packageName, null));
//            intent.putExtra(Intent.EXTRA_PACKAGES, new String[]{mAppEntry.info.packageName});
//            intent.putExtra(Intent.EXTRA_UID, mAppEntry.info.uid);
//            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppEntry.info.uid));
//            Log.d(TAG, "Sending broadcast to query restart status for "
//                    + mAppEntry.info.packageName);
//            mActivity.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
//                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);