package com.github.vatbub.hearingaid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.github.vatbub.hearingaid.fragments.CustomFragment;

import java.io.IOException;
import java.util.List;

import ru.noties.markwon.Markwon;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback, ProfileManager.ProfileManagerListener, AdapterView.OnItemSelectedListener {

    private final static String CURRENT_FRAGMENT_TAG_KEY = "currentFragmentTag";
    private final static String CURRENT_PROFILE_KEY = "currentProfile";
    private CustomFragment.FragmentTag currentFragmentTag;
    private ArrayAdapter<ProfileManager.Profile> profileAdapter;
    private boolean ignoreNextSpinnerSelection = false;

    public static void displayMarkdown(Activity activity, @RawRes int markdownFile, @IdRes int textViewToDisplayMarkdownIn) throws IOException {
        TextView textView = activity.findViewById(textViewToDisplayMarkdownIn);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        Markwon.unscheduleDrawables(textView);
        Markwon.unscheduleTableRows(textView);

        textView.setText(MarkdownRenderer.getInstance(activity).getCachedRenderResult(markdownFile));

        Markwon.scheduleDrawables(textView);
        Markwon.scheduleTableRows(textView);
    }

    public void ignoreNextSpinnerSelection() {
        ignoreNextSpinnerSelection = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RemoteConfig.initConfig(this);

        prerenderMarkdown();
        setTheme(R.style.AppTheme_NoActionBar);
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
            openFragment(CustomFragment.FragmentTag.STREAMING_FRAGMENT);
        } else {
            currentFragmentTag = CustomFragment.FragmentTag.valueOf(savedInstanceState.getString(CURRENT_FRAGMENT_TAG_KEY));
            updateSelectedItem(currentFragmentTag);
            updateTitle(currentFragmentTag);
        }

        Thread t = new Thread(() -> {
            SharedPreferences getPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());
            if (!getPrefs.getBoolean("firstStart", true))
                return;

            //  Launch app intro
            final Intent i = new Intent(MainActivity.this, IntroActivity.class);
            runOnUiThread(() -> startActivity(i));
            SharedPreferences.Editor e = getPrefs.edit();
            e.putBoolean("firstStart", false);
            e.apply();
        });

        t.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        initNavHeaderSpinner();
        Bundle savedInstanceState = getIntent().getExtras();

        if (savedInstanceState != null) {
            int currentlyActiveProfileId = savedInstanceState.getInt(CURRENT_PROFILE_KEY);
            if (currentlyActiveProfileId == -1 || !ProfileManager.getInstance(this).profileExists(currentlyActiveProfileId)) {
                List<ProfileManager.Profile> profiles = ProfileManager.getInstance(this).listProfiles();
                ProfileManager.Profile profileToApply;
                if (profiles.isEmpty())
                    profileToApply = ProfileManager.getInstance(this).createProfile("dummyProfile");
                else
                    profileToApply = profiles.get(0);

                ProfileManager.getInstance(this).applyProfile(profileToApply);
            } else {
                ignoreNextSpinnerSelection();
                ProfileManager.getInstance(this).applyProfile(currentlyActiveProfileId);
            }
        }
    }

    private void initNavHeaderSpinner() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        Spinner profileSelector = navigationView.getHeaderView(0).findViewById(R.id.nav_header_profile_selector);
        profileSelector.setAdapter(getProfileAdapter(true));
        ProfileManager.getInstance(this).getChangeListeners().add(this);
        profileSelector.setOnItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (currentFragmentTag == null || getCurrentFragment() == null || !getCurrentFragment().onBackPressed()) {
            try {
                CustomFragment.FragmentTag fragmentTagAboutToBeOpened = CustomFragment.FragmentTag.valueOf(getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 2).getName());
                Log.d(getClass().getName(), "Navigating back to fragment: " + fragmentTagAboutToBeOpened);
                updateTitle(fragmentTagAboutToBeOpened);
                updateSelectedItem(fragmentTagAboutToBeOpened);
                super.onBackPressed();
                currentFragmentTag = fragmentTagAboutToBeOpened;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(getClass().getName(), "onBackPressed: Unable to get the fragment about to be navigated to as there is no fragment in the back stack anymore", e);
                finish();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT_TAG_KEY, currentFragmentTag.toString());
        outState.putInt(CURRENT_PROFILE_KEY, ProfileManager.resetInstance(this));
        getIntent().putExtras(outState);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_streaming) {
            openFragment(CustomFragment.FragmentTag.STREAMING_FRAGMENT);
        } else if (id == R.id.nav_settings) {
            openFragment(CustomFragment.FragmentTag.SETTINGS_FRAGMENT);
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_message, RemoteConfig.getConfig().getValue(RemoteConfig.Keys.PLAY_STORE_URL)));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_screen_title)));
        } else if (id == R.id.nav_feedback) {
            startFeedbackActivity();
        } else if (id == R.id.nav_privacy) {
            openFragment(CustomFragment.FragmentTag.PRIVACY_FRAGMENT);
        } else if (id == R.id.nav_about) {
            openFragment(CustomFragment.FragmentTag.ABOUT_FRAGMENT);
        } else if (id == R.id.nav_github) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(RemoteConfig.getConfig().getValue(RemoteConfig.Keys.GIT_HUB_URL)));
            startActivity(browserIntent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void prerenderMarkdown() {
        BugsnagWrapper.leaveBreadcrumb("Prerendering markdown...");
        MarkdownRenderer.getInstance(this).prerender(R.raw.privacy);
        MarkdownRenderer.getInstance(this).prerender(R.raw.about);
    }

    private void openFragment(CustomFragment.FragmentTag tag) {
        // Insert the fragment by replacing any existing fragment
        /*FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content, initialFragmentInstance)
                .commit();
*/

        Log.d(getClass().getName(), "Opening fragment: " + tag);
        Fragment fragmentToUse = getSupportFragmentManager().findFragmentByTag(tag.toString());
        boolean fragmentFound = fragmentToUse != null;
        if (!fragmentFound)
            fragmentToUse = CustomFragment.createInstance(tag);

        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (currentFragmentTag != null && getCurrentFragment() != null)
            fragmentTransaction.hide(getCurrentFragment());

        if (fragmentFound)
            fragmentTransaction.show(fragmentToUse);
        else
            fragmentTransaction.add(R.id.content, fragmentToUse, tag.toString());

        fragmentTransaction.addToBackStack(tag.toString());
        fragmentTransaction.commit();
        currentFragmentTag = tag;

        updateTitle(tag);
    }

    public CustomFragment getCurrentFragment() {
        return (CustomFragment) getSupportFragmentManager().findFragmentByTag(currentFragmentTag.toString());
    }

    private void updateTitle(CustomFragment.FragmentTag fragmentTag) {
        switch (fragmentTag) {
            case STREAMING_FRAGMENT:
                setTitle(getString(R.string.fragment_streaming_title));
                break;
            case PRIVACY_FRAGMENT:
                setTitle(getString(R.string.fragment_privacy_title));
                break;
            case ABOUT_FRAGMENT:
                setTitle(getString(R.string.fragment_about_title));
                break;
            case SETTINGS_FRAGMENT:
                setTitle(getString(R.string.fragment_settings_title));
        }
    }

    private void updateSelectedItem(CustomFragment.FragmentTag fragmentTag) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        switch (fragmentTag) {
            case STREAMING_FRAGMENT:
                navigationView.setCheckedItem(R.id.nav_streaming);
                break;
            case PRIVACY_FRAGMENT:
                navigationView.setCheckedItem(R.id.nav_privacy);
                break;
            case ABOUT_FRAGMENT:
                navigationView.setCheckedItem(R.id.nav_about);
                break;
            case SETTINGS_FRAGMENT:
                navigationView.setCheckedItem(R.id.nav_settings);
        }
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter() {
        return getProfileAdapter(false);
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter(boolean forceRefresh) {
        if (profileAdapter == null) {
            profileAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_app_drawer);
            profileAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            initProfileAdapter();
        } else if (forceRefresh)
            initProfileAdapter();
        return profileAdapter;
    }

    public void initProfileAdapter() {
        getProfileAdapter().clear();
        getProfileAdapter().addAll(ProfileManager.getInstance(this).listProfiles());
    }

    @Override
    public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable final ProfileManager.Profile newProfile) {
        if (newProfile == null)
            return;

        NavigationView navigationView = findViewById(R.id.nav_view);
        Spinner profileSelector = navigationView.getHeaderView(0).findViewById(R.id.nav_header_profile_selector);
        int position = ProfileManager.getInstance(MainActivity.this).getPosition(newProfile);
        profileSelector.setSelection(position);
    }

    @Override
    public void onProfileCreated(ProfileManager.Profile newProfile) {
        getProfileAdapter().add(newProfile);
    }

    /**
     * Called just before a profile is deleted. Since the callback is called before the deletion of the profile, one can still access information from the profile in the callback.
     *
     * @param deletedProfile The profile about to be deleted
     */
    @Override
    public void onProfileDeleted(ProfileManager.Profile deletedProfile) {
        getProfileAdapter().remove(deletedProfile);
    }

    @Override
    public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {
        initProfileAdapter();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        if (ignoreNextSpinnerSelection) {
            ignoreNextSpinnerSelection = false;
            return;
        }
        ProfileManager.Profile selectedProfile = (ProfileManager.Profile) adapterView.getItemAtPosition(pos);
        ProfileManager.getInstance(this).applyProfile(selectedProfile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        System.out.println("Nothing selected");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getBooleanExtra(Constants.INTENT_COMING_FROM_NOTIFICATION_EXTRA_KEY, false))
            // go to the streaming fragment
            openFragment(CustomFragment.FragmentTag.STREAMING_FRAGMENT);
    }

    public void startFeedbackActivity() {
        Toast.makeText(this, "Feedback is currently disabled in this version.", Toast.LENGTH_LONG).show();
    }
}
