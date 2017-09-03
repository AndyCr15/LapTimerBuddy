package com.androidandyuk.laptimerbuddy;

/**
 * Created by AndyCr15 on 17/08/2017.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    private static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context arg0, Intent arg1) {

        Log.i(TAG,"Called");

        String thisLat = arg1.getStringExtra("Lat");
        String thisLon = arg1.getStringExtra("Lon");

        Log.i("thisLat(s)" + thisLat,"thisLon(s)" + thisLon);
        Location thisLocation = new Location("50,1");
        thisLocation.setLatitude(Double.parseDouble(thisLat));
        thisLocation.setLongitude(Double.parseDouble(thisLon));
        MainActivity.lastKnownLocation = thisLocation;
        // add a new market to the current session
        if(MainActivity.tracking) {
            new MainActivity().addMarker(thisLocation);
        }
    }
}
