package com.androidandyuk.laptimerbuddy;

import java.util.ArrayList;

/**
 * Created by AndyCr15 on 18/08/2017.
 */

public class Session {
    static int sessionCount;
    int ID;
    String notes;
    ArrayList<Marker> markers = new ArrayList<>();

    public Session() {
        sessionCount ++;
        this.ID = sessionCount;
    }

    public Session(int thisId, String thisNotes) {
        this.ID = thisId;
        this.notes = thisNotes;
        sessionCount = thisId + 1;
    }
}
