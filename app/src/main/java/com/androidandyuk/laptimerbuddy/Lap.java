package com.androidandyuk.laptimerbuddy;

/**
 * Created by AndyCr15 on 20/08/2017.
 */

public class Lap {
    int number;
    Long time;
    Double distance;
    Double topSpeed;
    Double aveSpeed;

    public Lap(int number, Long time, Double distance, Double topSpeed) {
        this.number = number;
        this.time = time;
        this.distance = distance;
        // store everything in Km, multiply by static variable 'conversion' if Miles is needed
        this.topSpeed = topSpeed;
        this.aveSpeed = distance * 3600000 / time;
    }
}
