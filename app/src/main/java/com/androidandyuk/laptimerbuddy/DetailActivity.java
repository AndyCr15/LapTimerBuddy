package com.androidandyuk.laptimerbuddy;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

import static com.androidandyuk.laptimerbuddy.MainActivity.activeSession;
import static com.androidandyuk.laptimerbuddy.MainActivity.conversion;
import static com.androidandyuk.laptimerbuddy.MainActivity.finishDirection;
import static com.androidandyuk.laptimerbuddy.MainActivity.finishLine;
import static com.androidandyuk.laptimerbuddy.MainActivity.finishRadius;
import static com.androidandyuk.laptimerbuddy.MainActivity.millisInMinutes;
import static com.androidandyuk.laptimerbuddy.MainActivity.navChoice;
import static com.androidandyuk.laptimerbuddy.MainActivity.oneDecimal;
import static com.androidandyuk.laptimerbuddy.MainActivity.saveSessions;
import static com.androidandyuk.laptimerbuddy.MainActivity.sessions;
import static com.androidandyuk.laptimerbuddy.MainActivity.unit;

public class DetailActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ArrayList<Lap> laps = new ArrayList<>();

    static MyLapAdapter myLapAdapter;
    ListView listView;

    public static Location topSpeedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView header = findViewById(R.id.header);
        header.setText("Session " + sessions.get(activeSession).ID);

        calculateLaps(sessions.get(activeSession));
        initiateList();
    }

    public void calculateLaps(Session session) {
        ArrayList<Marker> theseMarkers = session.markers;

        // calculate laps!!!
        if (theseMarkers.size() > 0) {
            for (Marker thisMarker : theseMarkers) {
                thisMarker.finishLine = false;
            }

            Double distance = 0d;
            Long lapStart = theseMarkers.get(0).timeStamp;
            Double topSpeed = 0d;
            int lapCounter = 0;

            laps.clear();

            Location finishLocation = new Location("0,0");
            finishLocation.setLatitude(finishLine.latitude);
            finishLocation.setLongitude(finishLine.longitude);

            Log.i("finishRadius", "" + finishRadius);

            for (int i = 1; i < theseMarkers.size(); i++) {
                Marker thisMarker = theseMarkers.get(i);
                Marker lastMarker = theseMarkers.get(i - 1);

                Double thisDistance = MapsActivity.getDistance(lastMarker.location, thisMarker.location);
                Long thisMillis = thisMarker.timeStamp - lastMarker.timeStamp;

//            Log.i("thisDistance " + thisDistance, "thisMillis " + thisMillis);

                distance += thisDistance;

                Double thisHours = (double) thisMillis / 3600000L;
                Double thisSpeed = (double) thisDistance / thisHours;


                if (thisSpeed > topSpeed && thisSpeed < 300) {
                    topSpeed = thisSpeed;
                    topSpeedLocation = thisMarker.location;
                }

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

                if (dif < 20 || dif > 340) {
                    headingHome = true;
                }

                if ((newToFinish < finishRadius) && (newToFinish < oldToFinish) && headingHome) {
                    // make this marker a point possibly a new lap has started
                    lastMarker.finishLine = false;
                    thisMarker.finishLine = true;
                }

                if (!thisMarker.finishLine && lastMarker.finishLine) {
                    // if the last marker was a possible new lap and this marker isn't, we must have passed the finish


                    Lap thisLap;
                    thisLap = new Lap(lapCounter, lastMarker.timeStamp - lapStart, distance, topSpeed);
                    laps.add(thisLap);

                    lapStart = lastMarker.timeStamp;

                    topSpeed = 0.0;
                    distance = 0.0;
                    lapCounter++;
                }

            }

            if (laps.size() > 0) {
                laps.remove(0);
            }

            // do the checks for fastest after the none laps have been removed
            if (laps.size() > 0) {
                for (Lap thisLap : laps) {
                    // check if this lap is the fastest of the session
                    if (thisLap.time < session.fastestLap) {
                        session.fastestLap = thisLap.time;
                    }
                    if (thisLap.topSpeed > session.topSpeed) {
                        session.topSpeed = thisLap.topSpeed;
                    }
                }
            }
        }
    }

    private void initiateList() {
        Log.i("Sessions", "Initiating List");
        if (laps.size() > 0) {
            listView = findViewById(R.id.lap_ListView);

            myLapAdapter = new MyLapAdapter(laps);

            listView.setAdapter(myLapAdapter);
        }
    }

    private class MyLapAdapter extends BaseAdapter {
        public ArrayList<Lap> lapDataAdapter;

        public MyLapAdapter(ArrayList<Lap> lapDataAdapter) {
            this.lapDataAdapter = lapDataAdapter;
        }

        @Override
        public int getCount() {
            return lapDataAdapter.size();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = getLayoutInflater();
            View myView = mInflater.inflate(R.layout.lap_listview, null);

            Lap s = lapDataAdapter.get(position);

            TextView lapNumber = myView.findViewById(R.id.lapNumber);
            lapNumber.setText("Lap " + s.number);

            TextView lapTime = myView.findViewById(R.id.lapTime);
            lapTime.setText(millisInMinutes(s.time));

            TextView aveSpeed = myView.findViewById(R.id.aveSpeed);
            aveSpeed.setText("Ave : " + oneDecimal.format(s.aveSpeed * conversion) + " " + unit);

            TextView topSpeed = myView.findViewById(R.id.topSpeed);
            topSpeed.setText("Top : " + oneDecimal.format(s.topSpeed * conversion) + " " + unit);

            return myView;
        }

    }

    public void viewOnMap(View view) {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        intent.putExtra("Type", "Markers");
        intent.putExtra("sessionNumber", activeSession);
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_timer) {

            navChoice = "timer";
            finish();

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

            navChoice = "";
            finish();

        } else if (id == R.id.nav_backup) {

            navChoice = "backupDB";
            finish();

        } else if (id == R.id.nav_restore) {

            navChoice = "restoreDB";
            finish();

        } else if (id == R.id.nav_delete) {

            new AlertDialog.Builder(DetailActivity.this)
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
                            Snackbar.make(findViewById(R.id.main), "Sessions Deleted", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                            navChoice = "timer";
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        calculateLaps(sessions.get(activeSession));
        initiateList();
        super.onResume();
    }
}
