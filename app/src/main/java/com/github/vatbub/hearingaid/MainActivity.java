package com.github.vatbub.hearingaid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.vatbub.hearingaid.fragments.AboutFragment;
import com.github.vatbub.hearingaid.fragments.PrivacyFragment;
import com.github.vatbub.hearingaid.fragments.SettingsFragment;
import com.github.vatbub.hearingaid.fragments.StreamingFragment;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.markdown4j.Markdown4jProcessor;
import org.rm3l.maoni.Maoni;
import org.rm3l.maoni.email.MaoniEmailListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback, ProfileManager.ActiveProfileChangeListener, AdapterView.OnItemSelectedListener {

    private final static String CURRENT_FRAGMENT_TAG_KEY = "currentFragmentTag";
    private final static String CURRENT_PROFILE_KEY = "currentProfile";
    private String currentFragmentTag;
    private ArrayAdapter<ProfileManager.Profile> profileAdapter;
    private boolean ignoreNextSpinnerSelection = false;

    public static void displayMarkdown(Resources resources, Activity activity, @RawRes int markdownFile, @IdRes int webViewToDisplayMarkdownIn) throws IOException {
        InputStream input = resources.openRawResource(markdownFile);
        List<String> lines = readLines(input);
        StringBuilder markdown = new StringBuilder();
        for (String line : lines) {
            markdown.append(line).append("\n");
        }

        String html = new Markdown4jProcessor().process(markdown.toString());
        ((WebView) activity.findViewById(webViewToDisplayMarkdownIn)).loadData(html, "text/html", "UTF-8");
    }

    public static List<String> readLines(InputStream input) throws IOException {
        InputStreamReader reader = new InputStreamReader(input);
        return readLines(reader);
    }

    /**
     * Get the contents of a <code>Reader</code> as a list of Strings,
     * one entry per line.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     *
     * @param input the <code>Reader</code> to read from, not null
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since Commons IO 1.1
     */
    private static List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        List<String> list = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            list.add(line);
            line = reader.readLine();
        }
        return list;
    }

    public void ignoreNextSpinnerSelection() {
        ignoreNextSpinnerSelection = true;
    }

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

        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (isFirstStart) {

                    //  Launch app intro
                    final Intent i = new Intent(MainActivity.this, IntroActivity.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(i);
                        }
                    });

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", false);

                    //  Apply changes
                    e.apply();
                }
            }
        });

        // Start the thread
        t.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        initNavHeaderSpinner();
        Bundle savedInstanceState = getIntent().getExtras();

        if (savedInstanceState != null) {
            int currentlyActiveProfileId = savedInstanceState.getInt(CURRENT_PROFILE_KEY);
            if (currentlyActiveProfileId == -1) {
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
        profileSelector.setAdapter(getProfileAdapter());
        ProfileManager.getInstance(this).getChangeListeners().add(this);
        profileSelector.setOnItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            try {
                String fragmentTagAboutToBeOpened = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 2).getName();
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
        outState.putInt(CURRENT_PROFILE_KEY, ProfileManager.resetInstance(this));
        getIntent().putExtras(outState);
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

        } else if (id == R.id.nav_settings) {
            openFragment("settingsFragment", new SettingsFragment());
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_message, FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.PLAY_STORE_URL)));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_screen_title)));
        } else if (id == R.id.nav_feedback) {
            startFeedbackActivity();
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
        /*FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content, initialFragmentInstance)
                .commit();
*/

        Log.d(getClass().getName(), "Opening fragment: " + tag);
        Fragment fragmentToUse = getSupportFragmentManager().findFragmentByTag(tag);
        boolean fragmentFound = fragmentToUse != null;
        if (!fragmentFound)
            fragmentToUse = initialFragmentInstance;

        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (currentFragmentTag != null && getSupportFragmentManager().findFragmentByTag(currentFragmentTag) != null)
            fragmentTransaction.hide(getSupportFragmentManager().findFragmentByTag(currentFragmentTag));

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
                setTitle(getString(R.string.fragment_streaming_title));
                break;
            case "privacyFragment":
                setTitle(getString(R.string.fragment_privacy_title));
                break;
            case "aboutFragment":
                setTitle(getString(R.string.fragment_about_title));
                break;
            case "settingsFragment":
                setTitle(getString(R.string.fragment_settings_title));
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
            case "settingsFragment":
                navigationView.setCheckedItem(R.id.nav_settings);
        }
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter() {
        if (profileAdapter == null) {
            profileAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_app_drawer);
            profileAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            initProfileAdapter();
        }
        return profileAdapter;
    }

    public void initProfileAdapter() {
        getProfileAdapter().clear();
        getProfileAdapter().addAll(ProfileManager.getInstance(this).listProfiles());
    }

    @Override
    public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable final ProfileManager.Profile newProfile) {
        if (newProfile == null)
            return;

        NavigationView navigationView = findViewById(R.id.nav_view);
        Spinner profileSelector = navigationView.getHeaderView(0).findViewById(R.id.nav_header_profile_selector);
        int position = ProfileManager.getInstance(MainActivity.this).getPosition(newProfile);
        profileSelector.setSelection(position);
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

    public void startFeedbackActivity() {
        final MaoniEmailListener listenerForMaoni = new MaoniEmailListener(this, FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.EMAIL_FEEDBACK_SUBJECT),
                FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.EMAIL_FEEDBACK_TO_ADDRESS));

        //The optional file provider authority allows you to
        //share the screenshot capture file to other apps (depending on your callback implementation)
        new Maoni.Builder(null)
                .withWindowTitle(getString(R.string.feedback_title)) //Set to an empty string to clear it
                .withMessage(getString(R.string.feedback_message))
                // .withExtraLayout(R.layout.my_feedback_activity_extra_content)
                // .withHandler(myHandlerForMaoni) //Custom Callback for Maoni
                .withListener(listenerForMaoni)
                .withFeedbackContentHint(getString(R.string.feedback_hint))
                .withIncludeScreenshotText(getString(R.string.feedback_inlude_screenshot))
                .withTouchToPreviewScreenshotText(getString(R.string.feedback_touch_to_preview_screenshot))
                .withContentErrorMessage(getString(R.string.feedback_error))
                .withScreenshotHint(getString(R.string.feedback_screenshot_hint))
                .disableLogsCapturingFeature()
                .disableScreenCapturingFeature()
                .build()
                .start(this);

    }
}
