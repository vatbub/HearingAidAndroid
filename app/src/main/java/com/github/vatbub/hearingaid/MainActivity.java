package com.github.vatbub.hearingaid;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.vatbub.hearingaid.fragments.AboutFragment;
import com.github.vatbub.hearingaid.fragments.PrivacyFragment;
import com.github.vatbub.hearingaid.fragments.StreamingFragment;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private final static String CURRENT_FRAGMENT_TAG_KEY = "currentFragmentTag";
    private String currentFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_streaming);
            openFragment("streamingFragment", new StreamingFragment());
        } else {
            currentFragmentTag = savedInstanceState.getString(CURRENT_FRAGMENT_TAG_KEY);
            updateSelectedItem(currentFragmentTag);
            updateTitle(currentFragmentTag);
        }

        RemoteConfig.initConfig();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            try {
                String fragmentTagAboutToBeOpened = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount() - 2).getName();
                Log.d(getClass().getName(), "Navigating back to fragment: " + fragmentTagAboutToBeOpened);
                updateTitle(fragmentTagAboutToBeOpened);
                updateSelectedItem(fragmentTagAboutToBeOpened);
                super.onBackPressed();
                currentFragmentTag = fragmentTagAboutToBeOpened;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(getClass().getName(), "onBackPressed: Unable to get the fragment about to be navigated to as there is no fragment in the back stack anymore", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT_TAG_KEY, currentFragmentTag);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_streaming) {
            openFragment("streamingFragment", new StreamingFragment());
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_message, FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.PLAY_STORE_URL)));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_screen_title)));
        } else if (id == R.id.nav_feedback) {

        } else if (id == R.id.nav_privacy) {
            openFragment("privacyFragment", new PrivacyFragment());
        } else if (id == R.id.nav_about) {
            openFragment("aboutFragment", new AboutFragment());
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openFragment(String tag, Fragment initialFragmentInstance) {
        // Insert the fragment by replacing any existing fragment
        /*FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content, initialFragmentInstance)
                .commit();
*/

        Log.d(getClass().getName(), "Opening fragment: " + tag);
        Fragment fragmentToUse = getFragmentManager().findFragmentByTag(tag);
        boolean fragmentFound = fragmentToUse != null;
        if (!fragmentFound)
            fragmentToUse = initialFragmentInstance;

        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (currentFragmentTag != null && getFragmentManager().findFragmentByTag(currentFragmentTag) != null)
            fragmentTransaction.hide(getFragmentManager().findFragmentByTag(currentFragmentTag));

        if (fragmentFound)
            fragmentTransaction.show(fragmentToUse);
        else
            fragmentTransaction.add(R.id.content, fragmentToUse, tag);

        fragmentTransaction.addToBackStack(tag);
        fragmentTransaction.commit();
        currentFragmentTag = tag;

        updateTitle(tag);
    }

    private void updateTitle(String fragmentTag) {
        switch (fragmentTag) {
            case "streamingFragment":
                setTitle(getString(R.string.fragment_streaming_titile));
                break;
            case "privacyFragment":
                setTitle(getString(R.string.fragment_privacy_titile));
                break;
            case "aboutFragment":
                setTitle(getString(R.string.fragment_about_title));
                break;
        }
    }

    private void updateSelectedItem(String fragmentTag) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        switch (fragmentTag) {
            case "streamingFragment":
                navigationView.setCheckedItem(R.id.nav_streaming);
                break;
            case "privacyFragment":
                navigationView.setCheckedItem(R.id.nav_privacy);
                break;
            case "aboutFragment":
                navigationView.setCheckedItem(R.id.nav_about);
                break;
        }
    }
}
