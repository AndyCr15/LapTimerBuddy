package com.androidandyuk.laptimerbuddy;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

import static com.androidandyuk.laptimerbuddy.MainActivity.finishDirection;
import static com.androidandyuk.laptimerbuddy.MainActivity.finishLine;
import static com.androidandyuk.laptimerbuddy.MainActivity.finishSet;
import static com.androidandyuk.laptimerbuddy.MainActivity.firstCorner;
import static com.androidandyuk.laptimerbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.laptimerbuddy.MainActivity.sessions;
import static com.androidandyuk.laptimerbuddy.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;

    public static String reason;
    public static int session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this);

        setMapMarkers();

        // read in the reason the map has been called
        Intent intent = getIntent();
        reason = intent.getStringExtra("Type");
        session = intent.getIntExtra("sessionNumber", -1);

        Log.i("Reason for Maps", reason);

        centerMapOnLocation(lastKnownLocation);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if (reason.equals("Markers")) {
                    viewSession();
                }
            }
        });

    }

    public void viewSession() {
        Log.i("viewSession", "Session :" + session);

        ArrayList<Marker> theseMarkers = sessions.get(session).markers;

        if (theseMarkers.size() > 2) {

            mMap.clear();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            Double distance = 0d;
            Long totalMillis = 0L;
            Double topSpeed = 0d;

            for (int i = 1; i < theseMarkers.size(); i++) {
                Log.i("MarkerCounter", "" + i);

                LatLng first = new LatLng(theseMarkers.get(i - 1).location.getLatitude(), theseMarkers.get(i - 1).location.getLongitude());
                LatLng second = new LatLng(theseMarkers.get(i).location.getLatitude(), theseMarkers.get(i).location.getLongitude());

                Double thisDistance = getDistance(theseMarkers.get(i - 1).location, theseMarkers.get(i).location);
                Long thisMillis = theseMarkers.get(i).timeStamp - theseMarkers.get(i - 1).timeStamp;

                Log.i("thisDistance " + thisDistance, "thisMillis " + thisMillis);

                distance += thisDistance;
                totalMillis += thisMillis;

                Double thisHours = (double) thisMillis / 3600000L;
                Double thisSpeed = (double) thisDistance / thisHours;

                if (thisSpeed > topSpeed && thisSpeed < 200) {
                    topSpeed = thisSpeed;
                }

                builder.include(second);

                int polyColour = Color.GRAY;
                if (thisSpeed > 20) {
                    polyColour = Color.rgb(141, 179, 139);
                }
                if (thisSpeed > 30) {
                    polyColour = Color.rgb(91, 202, 85);
                }
                if (thisSpeed > 40) {
                    polyColour = Color.rgb(100, 221, 23);
                }
                if (thisSpeed > 50) {
                    polyColour = Color.rgb(205, 220, 57);
                }
                if (thisSpeed > 60) {
                    polyColour = Color.rgb(233, 117, 40);
                }
                if (thisSpeed > 70) {
                    polyColour = Color.rgb(233, 69, 40);
                }
                if (thisSpeed > 80) {
                    polyColour = Color.rgb(198, 40, 40);
                }


                mMap.addPolyline(new PolylineOptions()
                        .add(first, second)
                        .width(15)
                        .color(polyColour)
                        .geodesic(true));

            }

            LatLngBounds bounds = builder.build();

            int padding = 50; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cu);

            Double totalHours = (double) totalMillis / 3600000L;
            Double aveSpeed = distance / totalHours;

            Log.i("distance " + distance, "totalHours " + totalHours);

//            tripDistance.setText("Distance : " + oneDecimal.format(distance) + " Miles");
//            tripTime.setText("Time : " + millisToHours(totalMillis));
//            tripAverage.setText("Ave Speed : " + oneDecimal.format(aveSpeed) + "mph");
//            tripTop.setText("Top Speed : " + oneDecimal.format(topSpeed) + "mph");


        } else {
            Snackbar.make(findViewById(R.id.mapView), "No Session To View", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        }
    }

    public static double getDistance(Location aa, Location bb) {
        double lat1 = bb.getLatitude();
        double lng1 = bb.getLongitude();
        double lat2 = aa.getLatitude();
        double lng2 = aa.getLongitude();

        Log.i("lat1 " + lat1,"lng1 " + lat2);
        Log.i("lat2 " + lat1,"lng2 " + lat2);

        int r = 6371; // average radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = r * c;
        d = d * 0.621;
        return d;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        if (reason.equals("Finish")) {
            if (finishSet) {

                firstCorner = latLng;
                finishSet = false;

            } else {

                finishLine = latLng;
                finishSet = true;

                Snackbar.make(findViewById(R.id.mapView), "Now Long Click Again To Show Direction", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();

            }

            setMapMarkers();
        }
    }

    public void setMapMarkers() {

        mMap.clear();

        if (firstCorner != null && !finishSet) {

            mMap.addPolyline(new PolylineOptions()
                    .add(finishLine, firstCorner)
                    .width(15)
                    .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow), 10))
                    .color(Color.LTGRAY)
                    .geodesic(true));

        }

        if (finishLine != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(finishLine)
                    .title("Finish Line")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.flag_marker)));

            mMap.addCircle(new CircleOptions()
                    .center(finishLine)
                    .radius(20)
                    .strokeColor(Color.LTGRAY)
                    .fillColor(Color.LTGRAY));

//            finishDirection = MainActivity.direction(firstCorner, finishLine);
            finishDirection = SphericalUtil.computeHeading(finishLine, firstCorner);
            Log.i("Finish Direction", "" + finishDirection);
        }
    }

    public void centreMapOnUserButton(View view) {
        mMap.clear();
        setMapMarkers();
        // Add a marker and move the camera
        LatLng you = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(you).title("Your Location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(you, 15));
    }

    public void centerMapOnLocation(Location location) {

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.i("centerMapOnLocation", "" + selectedLatLng);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
    }

    public void startLocationService() {

        Log.i("startLocationService", "" + lastKnownLocation);

        if (Build.VERSION.SDK_INT < 23) {

            Intent intent = new Intent(this, MyService.class);
            intent.putExtra("Time", 30000);
            intent.putExtra("Dist", 50);
            startService(intent);

        } else {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("Time", 30000);
                intent.putExtra("Dist", 50);
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
        startLocationService();
        super.onResume();
    }

    @Override
    protected void onPause() {
        stopLocationService();
        super.onPause();
    }
}
