package com.androidandyuk.laptimerbuddy;

import java.util.ArrayList;

/**
 * Created by AndyCr15 on 18/08/2017.
 */

public class Session {
    static int sessionCount;
    int ID;
    Double topSpeed;
    Long fastestLap;
    String notes;
    ArrayList<Marker> markers = new ArrayList<>();

    public Session() {
        sessionCount ++;
        this.ID = sessionCount;
        this.topSpeed = 0.0;
        this.fastestLap = 999999999L;
    }

    public Session(int thisId, String thisNotes) {
        this.ID = thisId;
        this.notes = thisNotes;
        this.topSpeed = 0.0;
        this.fastestLap = 999999999L;
        sessionCount = thisId + 1;
    }
}
