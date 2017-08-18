package com.androidandyuk.laptimerbuddy;

/**
 * Created by AndyCr15 on 17/08/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service
{
    private static final String TAG = "LOCATIONSERVICE";
    private LocationManager mLocationManager = null;

    int gpsTime;
    int gpsDist;

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);

        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationService: " + location);
            Log.i(TAG, "onLocationService: " + location);

            String thisLat = Double.toString(location.getLatitude());
            String thisLon = Double.toString(location.getLongitude());
            Log.i("thisLat(inS) " + thisLat, "thisLon(inS) " + thisLon);

            Intent intent = new Intent();
            intent.setAction("com.androidandyuk.laptimerbuddy");
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra("Lat", thisLat);
            intent.putExtra("Lon", thisLon);

            sendBroadcast(intent);

        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
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

        gpsTime = intent.getIntExtra("Time", 20000);
        gpsDist = intent.getIntExtra("Dist", 100);
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
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
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
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}