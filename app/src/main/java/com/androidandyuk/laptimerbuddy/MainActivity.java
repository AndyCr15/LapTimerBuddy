package com.androidandyuk.laptimerbuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final public static String TAG = "MainActivity";

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;

    public static Boolean tracking = false;
    public static Session currentSession;

    public static Double currentSpeed;

    public static LatLng finishLine = null;
    public static LatLng firstCorner;
    public static Double finishDirection;
    public static Double finishRadius = 0.05;
    public static Boolean finishSet = false;

    public static Boolean askedNearTrack = false;

    public static SQLiteDatabase lapTimerDB;

    public static int locationUpdatesTime = 0;
    public static int locationUpdatesDistance = 0;

    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
    public static SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");
    public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");

    public static final DecimalFormat precision = new DecimalFormat("0.00");
    public static final DecimalFormat oneDecimal = new DecimalFormat("0.#");
    public static final DecimalFormat noDecimal = new DecimalFormat("0");

    public static String jsonLocation = "http://www.androidandy.uk/json/";

    public static Boolean importingDB = true;
    public static String navChoice = "";
    private static final int CHOOSE_FILE_REQUESTCODE = 1;

    public static Timer timer;

    public static Location lastKnownLocation;

    public static ArrayList<Session> sessions = new ArrayList<>();
    public static int activeSession;

    public static Double conversion = 0.621371;
    public static String unit = "Mph";

    public static List<TrackLocation> trackLocations = new ArrayList<>();
    public static String nearestTrack;
    private static TextView nearestTrackTV;

    BroadcastReceiver mMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("com.androidandyuk.laptimerbuddy", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();
        loadSettings();

        nearestTrackTV = findViewById(R.id.nearestTrack);

        lastKnownLocation = new Location("1,50");

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tracking) {

                    Log.i("finishLine ", "" + finishLine);

                    if (!askedNearTrack) {
                        checkNearestTrack(lastKnownLocation, MainActivity.this);
                    }

                    Snackbar.make(view, "Location tracking started", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    tracking = true;
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_stop_watch_off));
                    // start tracking service
                    startLocationService();
                    TextView timer = (TextView) findViewById(R.id.timer);
                    timer.setText(millisInMinutes(0));
                    // I need to make a new session each time the timer is started
                    Session thisSession = new Session();
                    sessions.add(thisSession);
                    currentSession = sessions.get(sessions.size() - 1);
                    runTimer();
                } else {
                    Snackbar.make(view, "Location tracking stopped", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    tracking = false;
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_stop_watch));
                    //stop tracking service
                    timer.cancel();
                    timer.purge();
                    saveSessions();
                }
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        lapTimerDB = this.openOrCreateDatabase("sessions", MODE_PRIVATE, null);

        loadSessions();

        new MyAsyncTaskgetNews().execute(jsonLocation + "racetracks.json");


        IntentFilter iff = new IntentFilter(MyService.ACTION);
        this.registerReceiver(onNotice, iff);

        Intent i = new Intent(this, MyService.class);
        i.putExtra("Time", locationUpdatesTime);
        i.putExtra("Dist", locationUpdatesDistance);
        startService(i);

    }

    private BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity", "onReceive called");

            String thisLat = intent.getStringExtra("Lat");
            String thisLon = intent.getStringExtra("Lon");

            Log.i("thisLat(s)" + thisLat, "thisLon(s)" + thisLon);
            Location thisLocation = new Location("50,1");
            thisLocation.setLatitude(Double.parseDouble(thisLat));
            thisLocation.setLongitude(Double.parseDouble(thisLon));
            lastKnownLocation = thisLocation;
            // add a new market to the current session
            if (tracking) {
                addMarker(thisLocation);
            }
        }
    };

    public static void checkNearestTrack(Location lastKnownLocation, Context context) {
        if (trackLocations.size() > 0) {
            Log.i(TAG, "checkNearestTrack");
            for (final TrackLocation thisTrack : trackLocations) {
                if (thisTrack.getDistance(lastKnownLocation) < 2) {
                    Log.i(TAG, "Near track " + thisTrack);
                    if (thisTrack.finishLine.longitude != 0.0 && thisTrack.finishLine.latitude != 0.0) {
                        askedNearTrack = true;

                        new AlertDialog.Builder(context)
                                .setIcon(R.drawable.icon)
                                .setTitle("Are you at " + thisTrack.name + "?")
                                .setMessage("Would you like to use the pre-programmed track information?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        finishLine = thisTrack.finishLine;
                                        finishDirection = thisTrack.finishDir;
                                        Log.i("finishLine ", "" + finishLine);
                                        nearestTrack = thisTrack.name;
                                        nearestTrackTV.setText(nearestTrack);
                                    }
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }
                }
            }
        }
    }

    public static String lapCounter(int sessionNumber) {
        ArrayList<Lap> laps = new ArrayList<>();
        ArrayList<Marker> theseMarkers = sessions.get(sessionNumber).markers;

        // start on -1 as the first time past the finish line is the start of the first lap and doesn't count
        int lapCounter = -1;

        Location finishLocation = new Location("0,0");
        finishLocation.setLatitude(finishLine.latitude);
        finishLocation.setLongitude(finishLine.longitude);

        Log.i("finishRadius", "" + finishRadius);

        for (int i = 1; i < theseMarkers.size(); i++) {
            Marker thisMarker = theseMarkers.get(i);
            Marker lastMarker = theseMarkers.get(i - 1);

            Double newToFinish = MapsActivity.getDistance(finishLocation, thisMarker.location);
            Double oldToFinish = MapsActivity.getDistance(finishLocation, lastMarker.location);

            LatLng thisLatLng = new LatLng(thisMarker.location.getLatitude(), thisMarker.location.getLongitude());
            LatLng lastLatLng = new LatLng(lastMarker.location.getLatitude(), lastMarker.location.getLongitude());
            Double dir = SphericalUtil.computeHeading(lastLatLng, thisLatLng);

            Boolean headingHome = false;
            Double finishDir = finishDirection;

            if (dir < 0) {
                dir = 360 + dir;
            }

            if (finishDir < 0) {
                finishDir = 360 + finishDirection;
            }

            Double dif = Math.abs(dir - finishDir);

            if (dif < 30 || dif > 330) {
                headingHome = true;
            }

            if ((newToFinish < finishRadius) && (newToFinish < oldToFinish) && headingHome) {
                // make this marker a point possibly a new lap has started
                lastMarker.finishLine = false;
                thisMarker.finishLine = true;
            }

            if (!thisMarker.finishLine && lastMarker.finishLine) {
                // if the last marker was a possible new lap and this marker isn't, we must have passed the finish
                lapCounter++;
            }

        }

        if (lapCounter < 0) {
            return "Not Started a Lap";
        }
        return "Lap Count: " + Integer.toString(lapCounter);

    }

    public void runTimer() {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if (currentSession.markers.size() > 1) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // can't pause the timer, as it's based on the first marker of the session
                            Long currentTimer = System.currentTimeMillis() - currentSession.markers.get(1).timeStamp;
                            TextView timer = (TextView) findViewById(R.id.timer);
                            TextView direction = (TextView) findViewById(R.id.directionTV);
                            timer.setText(millisInMinutes(currentTimer));

                            Marker thisMarker = currentSession.markers.get(currentSession.markers.size() - 1);
                            Marker lastMarker = currentSession.markers.get(currentSession.markers.size() - 2);

                            LatLng thisLatLng = new LatLng(thisMarker.location.getLatitude(), thisMarker.location.getLongitude());
                            LatLng lastLatLng = new LatLng(lastMarker.location.getLatitude(), lastMarker.location.getLongitude());
                            Double dir = SphericalUtil.computeHeading(lastLatLng, thisLatLng);
                            if (dir < 0) {
                                dir = 360 + dir;
                            }
//                            Double dir = direction(currentSession.markers.get(currentSession.markers.size() - 1).location, currentSession.markers.get(currentSession.markers.size() - 2).location);
                            direction.setText(Html.fromHtml("Direction: " + oneDecimal.format(dir) + "<sup><small>o</small></sup>"));

                            Double thisDistance = MapsActivity.getDistance(lastMarker.location, thisMarker.location);
                            Long thisMillis = thisMarker.timeStamp - lastMarker.timeStamp;

                            Double thisHours = (double) thisMillis / 3600000L;
                            Double thisSpeed = (double) thisDistance / thisHours;

                            TextView speed = findViewById(R.id.speedTV);
                            speed.setText(oneDecimal.format(thisSpeed) + unit);

                            TextView lapCount = findViewById(R.id.lapCounter);
                            lapCount.setText(lapCounter(sessions.size() - 1));

                        }

                    });
                }
            }
        }, 0, 125);

    }

    public static String millisInMinutes(long milliSeconds) {
        String mins = Long.toString(milliSeconds / 60000);
        String secs = Long.toString((milliSeconds % 60000) / 1000);
        if (secs.length() < 2) {
            secs = "0" + secs;
        }
        String millis = Long.toString(milliSeconds % 1000);
        String formatedMillis = ("000" + millis).substring(millis.length(), millis.length() + 2);

        String value = mins + ":" + secs + ":" + formatedMillis;

        return value;
    }

    public static String millisToTime(Long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return dateTimeFormatter.format(calendar.getTime());
    }

    public void checkFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);
        if (!tracking) {
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_stop_watch));
        } else {
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_stop_watch_off));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                startLocationService();

            }

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Log.i("onRequest", "Write Storage permission granted");

                if (importingDB) {
                    importDB();
                } else {
                    showFileName();
                }


            }
        }

    }

    public void addMarker(Location thisLocation) {
        lastKnownLocation = thisLocation;
        Log.i("addMarker", "Location Received" + lastKnownLocation);
        currentSession.markers.add(new Marker(lastKnownLocation));
//        updateTimer();

    }

    public static Double direction(Location locB, Location locA) {
        Double lat1 = locA.getLatitude();
        Double lon1 = locA.getLongitude();
        Double lat2 = locB.getLatitude();
        Double lon2 = locB.getLongitude();

        Double dLon = Math.toRadians(lon2 - lon1);
        Double dPhi = Math.log(
                Math.tan(Math.toRadians(lat2) / 2 + Math.PI / 4) / Math.tan(Math.toRadians(lat1) / 2 + Math.PI / 4));
        if (Math.abs(dLon) > Math.PI)
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        return ToBearing(Math.atan2(dLon, dPhi));
    }

    public static Double direction(LatLng locB, LatLng locA) {
        Double lat1 = locA.latitude;
        Double lon1 = locA.longitude;
        Double lat2 = locB.latitude;
        Double lon2 = locB.longitude;

        Double dLon = Math.toRadians(lon2 - lon1);
        Double dPhi = Math.log(
                Math.tan(Math.toRadians(lat2) / 2 + Math.PI / 4) / Math.tan(Math.toRadians(lat1) / 2 + Math.PI / 4));
        if (Math.abs(dLon) > Math.PI)
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        return ToBearing(Math.atan2(dLon, dPhi));
    }

    public static double ToDegrees(double radians) {
        return radians * 180 / Math.PI;
    }

    public static double ToBearing(double radians) {
        // convert radians to degrees (as bearing: 0...360)
        return (ToDegrees(radians) + 360) % 360;
    }


    public static void saveSessions() {

        ArrayList<String> ID = new ArrayList<>();
        ArrayList<String> notes = new ArrayList<>();

        for (Session thisSession : sessions) {

            Log.i("Saving Sessions", "ID: " + thisSession.ID);

            ID.add(Integer.toString(thisSession.ID));
            notes.add(thisSession.notes);
        }
        Log.i("saveSession", "Size :" + ID.size());

        try {

            lapTimerDB.execSQL("CREATE TABLE IF NOT EXISTS sessions (ID VARCHAR, notes VARCHAR)");

            lapTimerDB.delete("sessions", null, null);

            lapTimerDB.execSQL("INSERT INTO sessions (ID, notes) VALUES ('" + ObjectSerializer.serialize(ID) + "' , '" + ObjectSerializer.serialize(notes) + "')");

        } catch (Exception e) {

            e.printStackTrace();
        }

        saveMarkers();
    }

    public static void saveMarkers() {

        for (Session thisSession : sessions) {

            Log.i("Saving Markers", "session.ID" + thisSession.ID);

            ArrayList<String> lat = new ArrayList<>();
            ArrayList<String> lon = new ArrayList<>();
            ArrayList<String> timeStamp = new ArrayList<>();
            ArrayList<String> finishLine = new ArrayList<>();

            for (Marker thisMarker : thisSession.markers) {

                lat.add(Double.toString(thisMarker.location.getLatitude()));
                lon.add(Double.toString(thisMarker.location.getLongitude()));
                timeStamp.add(Long.toString(thisMarker.timeStamp));
                finishLine.add(String.valueOf(thisMarker.finishLine));

            }
            Log.i("saveMarkers", "Size :" + lat.size());

            try {
                String dbname = "markers" + thisSession.ID;

                lapTimerDB.execSQL("CREATE TABLE IF NOT EXISTS '" + dbname + "' (lat VARCHAR, lon VARCHAR, timeStamp VARCHAR, finishLine VARCHAR)");

                lapTimerDB.delete(dbname, null, null);

                lapTimerDB.execSQL("INSERT INTO '" + dbname + "' (lat, lon, timeStamp, finishLine) VALUES ('" +
                        ObjectSerializer.serialize(lat) + "' , '" + ObjectSerializer.serialize(lon) + "' , '" + ObjectSerializer.serialize(timeStamp) + "' , '" +
                        ObjectSerializer.serialize(finishLine) + "')");

            } catch (Exception e) {

                e.printStackTrace();
                Log.i("Error", "savingMarkers");

            }
        }
    }

    public static void loadSessions() {

        sessions.clear();

        try {

            Cursor c = lapTimerDB.rawQuery("SELECT * FROM sessions", null);

            int IDIndex = c.getColumnIndex("ID");
            int notesIndex = c.getColumnIndex("notes");

            c.moveToFirst();

            do {

                ArrayList<String> ID = new ArrayList<>();
                ArrayList<String> notes = new ArrayList<>();

                try {

                    ID = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(IDIndex));
                    notes = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(notesIndex));

                    Log.i("Sessions Restored ", "Count :" + ID.size());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Loading Sessions", "Failed attempt");
                }

                Log.i("Retrieved info", "Session count :" + ID.size());
                if (ID.size() > 0 && notes.size() > 0) {
                    // we've checked there is some info
                    if (ID.size() == notes.size()) {
                        // we've checked each item has the same amount of info, nothing is missing
                        for (int x = 0; x < ID.size(); x++) {
                            int thisId = Integer.parseInt(ID.get(x));
                            String thisNotes = notes.get(x);
                            Session newSession = new Session(thisId, thisNotes);
                            Log.i("Adding", " " + x + " " + newSession);
                            sessions.add(newSession);
                        }
                    }
                }
            } while (c.moveToNext());

        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }
        loadMarkers();
    }

    public static void loadMarkers() {

        for (Session thisSession : sessions) {
            thisSession.markers.clear();

            try {

                String dbname = "markers" + thisSession.ID;

                Cursor c = lapTimerDB.rawQuery("SELECT * FROM " + dbname, null);

                int latIndex = c.getColumnIndex("lat");
                int lonIndex = c.getColumnIndex("lon");
                int timeStampIndex = c.getColumnIndex("timeStamp");
                int finishLineIndex = c.getColumnIndex("finishLine");

                c.moveToFirst();

                do {

                    ArrayList<String> lat = new ArrayList<>();
                    ArrayList<String> lon = new ArrayList<>();
                    ArrayList<String> timeStamp = new ArrayList<>();
                    ArrayList<String> finishLine = new ArrayList<>();

                    try {

                        lat = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(latIndex));
                        lon = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(lonIndex));
                        timeStamp = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(timeStampIndex));
                        finishLine = (ArrayList<String>) ObjectSerializer.deserialize(c.getString(finishLineIndex));

                        Log.i("Markers Restored ", "Count :" + lat.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("Loading Markers", "Failed attempt");
                    }

                    Log.i("Retrieved info", "Marker count :" + lat.size());
                    if (lat.size() > 0 && lon.size() > 0 && finishLine.size() > 0) {
                        // we've checked there is some info
                        if (lat.size() == lon.size() && timeStamp.size() == finishLine.size()) {
                            // we've checked each item has the same amount of info, nothing is missing
                            for (int x = 0; x < lat.size(); x++) {
                                Location thisLocation = new Location("0,0");
                                thisLocation.setLatitude(Double.parseDouble(lat.get(x)));
                                thisLocation.setLongitude(Double.parseDouble(lon.get(x)));

                                Marker newMarker = new Marker(thisLocation, Long.parseLong(timeStamp.get(x)), Boolean.valueOf(finishLine.get(x)));

                                thisSession.markers.add(newMarker);
                            }
                        }
                    }
                } while (c.moveToNext());

            } catch (Exception e) {

                Log.i("LoadingDB", "Caught Error");
                e.printStackTrace();
            }
        }
    }

    public static void loadSettings() {

        Log.i("loadSettings", "Running");

        Double latitude = Double.parseDouble(sharedPreferences.getString("finishLineLat", "0"));
        Double longitude = Double.parseDouble(sharedPreferences.getString("finishLineLon", "0"));
        finishLine = new LatLng(latitude, longitude);

        Double fclatitude = Double.parseDouble(sharedPreferences.getString("finishLineLat", "0"));
        Double fclongitude = Double.parseDouble(sharedPreferences.getString("finishLineLon", "0"));
        firstCorner = new LatLng(fclatitude, fclongitude);

        finishDirection = Double.parseDouble(sharedPreferences.getString("finishDirection", "0"));
        finishRadius = Double.parseDouble(sharedPreferences.getString("finishRadius", "0.1"));

    }

    public static void saveSettings() {

        Log.i("saveSettings", "Running");

        Double finLat = finishLine.latitude;
        Double finLon = finishLine.longitude;
        ed.putString("finishLineLat", finLat.toString()).apply();
        ed.putString("finishLineLon", finLon.toString()).apply();

        Double corLat = firstCorner.latitude;
        Double corLon = firstCorner.longitude;
        ed.putString("firstCornerLat", corLat.toString()).apply();
        ed.putString("firstCornerLon", corLon.toString()).apply();

        ed.putString("finishDirection", finishDirection.toString()).apply();
        ed.putString("finishRadius", finishRadius.toString()).apply();

    }

    public void shieldClicked(View view) {
        LinearLayout fileNameLL = findViewById(R.id.fileNameLL);
        if (fileNameLL.isShown()) {
            hideFileName();
        }
    }

    public void showFileName(){
        // TO-DO add shield and back press check
        LinearLayout fileNameLL = findViewById(R.id.fileNameLL);
        fileNameLL.setVisibility(View.VISIBLE);
        ImageView shield = findViewById(R.id.shield);
        shield.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void hideFileName(){
        ImageView shield = findViewById(R.id.shield);
        LinearLayout fileNameLL = findViewById(R.id.fileNameLL);
        fileNameLL.setVisibility(View.INVISIBLE);
        shield.setVisibility(View.INVISIBLE);
        // hide keyboard
        View thisView = this.getCurrentFocus();
        if (thisView != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(thisView.getWindowToken(), 0);
        }
    }

    public void backupDB() {
        importingDB = true;
        int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("exportTrip", "storage !=");
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            Log.i("exportTrip", "listPermissionsNeeded !=");
        } else {
            Log.i("exportTrip", "exporting!");
            showFileName();
        }
    }

    public void fileNameClicked(View view){
        EditText fileNameET = findViewById(R.id.fileNameET);
        String fileName = fileNameET.getText().toString();
        // removes characters that shouldn't be in a filename
        if (fileName.matches(".*['^*&%\\s.+$'].*")) {
            fileName = fileName.replaceAll("['^*&%\\s.+$']", "");
        }
        // save it lower case
        exportDB(fileName.toLowerCase());
        hideFileName();
    }

    public void exportDB(String fileName) {
        Log.i("exportDB", "Starting");
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        final FileChannel[] source = {null};
        final FileChannel[] destination = {null};

        File dir = new File(Environment.getExternalStorageDirectory() + "/LapTimerBuddy/");
        try {
            if (dir.mkdir()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Directory is not created");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Creating Dir Error", "" + e);
        }

        String currentDBPath = "/data/com.androidandyuk.laptimerbuddy/databases/sessions";
        String backupDBPath = "LapTimerBuddy/" + fileName + ".db";
        final File currentDB = new File(data, currentDBPath);
        final File backupDB = new File(sd, backupDBPath);

        if(backupDB.exists()){

            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("File Already Exists")
                    .setMessage("Would you like to overwrite this file?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            try {
                                source[0] = new FileInputStream(currentDB).getChannel();
                                destination[0] = new FileOutputStream(backupDB).getChannel();
                                destination[0].transferFrom(source[0], 0, source[0].size());
                                source[0].close();
                                destination[0].close();
                                Snackbar.make(findViewById(R.id.main), "DB Exported!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Snackbar.make(findViewById(R.id.main), "Exported Failed!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                            }

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            showFileName();

                        }
                    })
                    .show();

        }

    }

    public void restoreDB() {
        importingDB = true;
        int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("importTrip", "storage !=");
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            Log.i("importTrip", "listPermissionsNeeded !=");
        } else {
            Log.i("importTrip", "importing!");
            importDB();
        }
    }

    public void importDB() {

        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/LapTimerBuddy/");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setDataAndType(selectedUri, "*/db");
        Intent i = Intent.createChooser(intent, "File");
        startActivityForResult(i, CHOOSE_FILE_REQUESTCODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode){
            case CHOOSE_FILE_REQUESTCODE:
                if(resultCode==-1){
                    Uri uri = data.getData();
                    String yourDbFileNamePresentInSDCard = uri.getPath();



                    Log.i("ImportDB", "Started");
                    try {
                        String DB_PATH = "/data/data/com.androidandyuk.laptimerbuddy/databases/sessions";

//                        File sdcard = Environment.getExternalStorageDirectory();
//                        yourDbFileNamePresentInSDCard = sdcard.getAbsolutePath() + File.separator + "LapTimerBuddy/LapTimer.db";

                        Log.i("ImportDB", "SDCard File " + yourDbFileNamePresentInSDCard);

                        File file = new File(yourDbFileNamePresentInSDCard);
                        // Open your local db as the input stream
                        InputStream myInput = new FileInputStream(file);

                        // Path to created empty db
                        String outFileName = DB_PATH;

                        // Opened assets database structure
                        OutputStream myOutput = new FileOutputStream(outFileName);

                        // transfer bytes from the inputfile to the outputfile
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = myInput.read(buffer)) > 0) {
                            myOutput.write(buffer, 0, length);
                        }

                        // Close the streams
                        myOutput.flush();
                        myOutput.close();
                        myInput.close();
                    } catch (Exception e) {
                        Log.i("ImportDB", "Exception Caught" + e);
                    }
                    loadSessions();
                    Snackbar.make(findViewById(R.id.main), "DB Imported", Snackbar.LENGTH_SHORT).setAction("Action", null).show();

                }
                break;
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    public void importDBold() {
        Log.i("ImportDB", "Started");
        try {
            String DB_PATH = "/data/data/com.androidandyuk.laptimerbuddy/databases/sessions";

            File sdcard = Environment.getExternalStorageDirectory();
            String yourDbFileNamePresentInSDCard = sdcard.getAbsolutePath() + File.separator + "LapTimerBuddy/LapTimer.db";

            Log.i("ImportDB", "SDCard File " + yourDbFileNamePresentInSDCard);

            File file = new File(yourDbFileNamePresentInSDCard);
            // Open your local db as the input stream
            InputStream myInput = new FileInputStream(file);

            // Path to created empty db
            String outFileName = DB_PATH;

            // Opened assets database structure
            OutputStream myOutput = new FileOutputStream(outFileName);

            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            Log.i("ImportDB", "Exception Caught" + e);
        }
        loadSessions();
        Snackbar.make(findViewById(R.id.main), "DB Imported", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout fileNameLL = findViewById(R.id.fileNameLL);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fileNameLL.isShown()) {
            hideFileName();
        }else if (tracking) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_timer) {

//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            startActivity(intent);

        } else if (id == R.id.nav_map) {

            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra("Type", "Finish");
            startActivity(intent);

        } else if (id == R.id.nav_social) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_settings) {

            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_sessions) {

            Intent intent = new Intent(getApplicationContext(), SessionsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_backup) {

            backupDB();

        } else if (id == R.id.nav_restore) {

            restoreDB();

        } else if (id == R.id.nav_delete) {

            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Are you sure?")
                    .setMessage("You're about to delete all your sessions forever...")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i("Removing", "Sessions");
                            sessions.clear();
                            Session.sessionCount = 0;
                            saveSessions();
                            saveMarkers();
                            Snackbar.make(findViewById(R.id.main), "Sessions Deleted", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startLocationService() {

        Log.i("startLocationService", "" + lastKnownLocation);

        if (Build.VERSION.SDK_INT < 23) {

            Intent intent = new Intent(this, MyService.class);
            intent.putExtra("Time", locationUpdatesTime);
            intent.putExtra("Dist", locationUpdatesDistance);
            startService(intent);

        } else {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("Time", locationUpdatesTime);
                intent.putExtra("Dist", locationUpdatesDistance);
                Log.i("startLocationService", "" + locationUpdatesTime);
                startService(intent);

//                lastKnownLocation.setLatitude(1d);
//                lastKnownLocation.setLongitude(50d);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        }

    }

    public void stopLocationService() {
        try {
            // Unregister since the activity is about to be closed.
            Intent intent = new Intent(getApplicationContext(), MyService.class);
            stopService(intent);
            unregisterReceiver(onNotice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        saveSettings();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFAB();
        switch (navChoice) {
            case "backupDB":
                navChoice = "";
                backupDB();
                return;
            case "restoreDB":
                navChoice = "";
                restoreDB();
                return;
            case "timer":
                navChoice = "";
                return;
        }
    }

    @Override
    protected void onDestroy() {
        if (!tracking) {
            stopLocationService();
        }
        super.onDestroy();
    }
}
