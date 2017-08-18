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

    public double getDistance(Marker o) {
        if (o != null && this != o) {
            double lat1 = this.location.getLatitude();
            double lng1 = this.location.getLongitude();
            double lat2 = o.location.getLatitude();
            double lng2 = o.location.getLongitude();

            int r = 6371; // average radius of the earth in km
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double d = r * c;
            // return meters
            return d;
        }
        return 0;
    }

}
