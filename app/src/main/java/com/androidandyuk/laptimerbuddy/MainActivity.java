package com.androidandyuk.laptimerbuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static com.androidandyuk.laptimerbuddy.Session.sessionCount;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;

    public static Boolean tracking = false;
    public static Session currentSession;

    public static LatLng finishLine;
    public static LatLng firstCorner;
    public static Double finishDirection;
    public static Boolean finishSet = false;

    public static SQLiteDatabase lapTimerDB;

    public static LocationManager locationManager;
    public static LocationListener locationListener;
    public static int locationUpdatesTime = 0;
    public static int locationUpdatesDistance = 0;

    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
    public static SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");
    public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");

    public static final DecimalFormat precision = new DecimalFormat("0.00");
    public static final DecimalFormat oneDecimal = new DecimalFormat("0.#");

    public static Timer timer;

    public static Location lastKnownLocation;

    public static ArrayList<Session> sessions = new ArrayList<>();
    public static int activeSession;

    public static Double conversion = 0.621371;
    public static String unit = "Miles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("com.androidandyuk.laptimerbuddy", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        loadSettings();

        lastKnownLocation = new Location("1,50");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tracking) {
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
                    stopLocationService();
                    timer.cancel();
                    timer.purge();
                    saveSessions();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        lapTimerDB = this.openOrCreateDatabase("sessions", MODE_PRIVATE, null);

        loadSessions();

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
                            Double dir = direction(currentSession.markers.get(currentSession.markers.size() - 1).location, currentSession.markers.get(currentSession.markers.size() - 2).location);
                            direction.setText(Html.fromHtml("Direction: " + oneDecimal.format(dir) + "<sup><small>o</small></sup>"));

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                startLocationService();

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
                                Log.i("Added", " " + x + " " + newMarker);
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

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

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

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_sessions) {

            Intent intent = new Intent(getApplicationContext(), SessionsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_backup) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // saveDB

        } else if (id == R.id.nav_restore) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // loadDB

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
                            sessionCount = 0;
                            Snackbar.make(findViewById(R.id.sessions_ListView), "Sessions Deleted", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
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
            Intent intent = new Intent(getApplicationContext(), MyService.class);
            stopService(intent);
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
    }
}
