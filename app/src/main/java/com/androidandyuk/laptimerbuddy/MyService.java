package com.androidandyuk.laptimerbuddy;

/**
 * Created by AndyCr15 on 17/08/2017.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service
{
    private static final String TAG = "LOCATIONSERVICE";
    public static final String ACTION = "mycustomactionstring";
    private LocationManager mLocationManager = null;

    int gpsTime;
    int gpsDist;

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider) {

            Log.i(TAG, "LocationListener " + provider);

        }

        @Override
        public void onLocationChanged(Location location) {

            Log.i(TAG, "onLocationChanged :" + location);

            String thisLat = Double.toString(location.getLatitude());
            String thisLon = Double.toString(location.getLongitude());
            Log.i("thisLat(inS) " + thisLat, "thisLon(inS) " + thisLon);

            Intent intent = new Intent(ACTION);
            intent.putExtra("Lat", thisLat);
            intent.putExtra("Lon", thisLon);

            sendBroadcast(intent);

        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.i(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.i(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.i(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        gpsTime = intent.getIntExtra("Time", 0);
        gpsDist = intent.getIntExtra("Dist", 0);
        Log.i("gpsTime " + gpsTime,"gpsDist " + gpsDist);

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, gpsTime, gpsDist,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }


        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");

        int notificationID = 100;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the channel.
            String id = "my_channel_01";
            // The user-visible name of the channel.
            CharSequence name = "Channel Name";
            // The user-visible description of the channel.
            String description = "Channel Desc";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.BLUE);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(mChannel);


            // Create a notification and set the notification channel.
            Notification.Builder notification = new Notification.Builder(this)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("Tracking location")
                    .setSmallIcon(R.drawable.ic_stop_watch)
                    .setChannelId(id)
                    .setAutoCancel(true);

            startForeground(1337, notification.build());

        } else {

            Notification.Builder notification = new Notification.Builder(this)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("Tracking location")
                    .setSmallIcon(R.drawable.ic_stop_watch)
                    .setAutoCancel(true);

            startForeground(1337, notification.build());
        }

        initializeLocationManager();
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.i(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}