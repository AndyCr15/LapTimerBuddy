package com.androidandyuk.laptimerbuddy;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import static com.androidandyuk.laptimerbuddy.MainActivity.activeSession;
import static com.androidandyuk.laptimerbuddy.MainActivity.sessions;

public class DetailActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
    }

    public void viewOnMap(View view){
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        intent.putExtra("Type", "Markers");
        intent.putExtra("sessionNumber", activeSession);
        startActivity(intent);
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

            Intent intent = new Intent(getApplicationContext(), SessionsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_backup) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // saveDB

        } else if (id == R.id.nav_restore) {

            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show();
            // loadDB

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
