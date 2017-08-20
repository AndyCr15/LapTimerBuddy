package com.androidandyuk.laptimerbuddy;

import android.location.Location;

/**
 * Created by AndyCr15 on 17/08/2017.
 */

public class Marker {
    Location location;
    Long timeStamp;
    Boolean finishLine;

    public Marker(Location location) {
        this.location = location;
        this.timeStamp = System.currentTimeMillis();
        this.finishLine = false;
    }

    public Marker(Location location, Long millis, Boolean finishLine) {
        this.location = location;
        this.timeStamp = millis;
        this.finishLine = finishLine;
    }

}
