package com.androidandyuk.laptimerbuddy;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.androidandyuk.laptimerbuddy.MainActivity.activeSession;
import static com.androidandyuk.laptimerbuddy.MainActivity.millisToTime;
import static com.androidandyuk.laptimerbuddy.MainActivity.sessions;
import static com.androidandyuk.laptimerbuddy.Session.sessionCount;

public class SessionsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static MySessionAdapter myAdapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initiateList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Log.i("listView","Position " + position);
                activeSession = position;
                Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                startActivity(intent);

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                new AlertDialog.Builder(SessionsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("You're about to delete this session forever...")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Removing", "Session " + position);
                                sessions.remove(position);
                                initiateList();
//                                Toast.makeText(getApplicationContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                                Snackbar.make(findViewById(R.id.sessions_ListView), "Session Deleted", Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }

        });

    }

    private void initiateList() {
        Log.i("Sessions", "Initiating List");
        listView = findViewById(R.id.sessions_ListView);

        myAdapter = new MySessionAdapter(sessions);

        listView.setAdapter(myAdapter);

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_timer) {

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_map) {

            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra("Type", "Finish");
            startActivity(intent);

        } else if (id == R.id.nav_social) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_settings) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_sessions) {

//            Intent intent = new Intent(getApplicationContext(), SessionsActivity.class);
//            startActivity(intent);

        } else if (id == R.id.nav_backup) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // saveDB

        } else if (id == R.id.nav_restore) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // loadDB

        } else if (id == R.id.nav_delete) {

            new AlertDialog.Builder(SessionsActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Are you sure?")
                    .setMessage("You're about to delete all your sessions forever...")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i("Removing", "Sessions");
                            sessions.clear();
                            sessionCount = 0;
                            initiateList();
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

    private class MySessionAdapter extends BaseAdapter {
        public ArrayList<Session> sessionDataAdapter;

        public MySessionAdapter(ArrayList<Session> sessionDataAdapter) {
            this.sessionDataAdapter = sessionDataAdapter;
        }

        @Override
        public int getCount() {
            return sessionDataAdapter.size();
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
            View myView = mInflater.inflate(R.layout.session_listview, null);

            Session s = sessionDataAdapter.get(position);

            TextView sessionNumber = (TextView) myView.findViewById(R.id.sessionNumber);
            sessionNumber.setText("Session " + s.ID);

            TextView dateTime = (TextView) myView.findViewById(R.id.dateTime);
            dateTime.setText(millisToTime(s.markers.get(0).timeStamp));

            return myView;
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
}