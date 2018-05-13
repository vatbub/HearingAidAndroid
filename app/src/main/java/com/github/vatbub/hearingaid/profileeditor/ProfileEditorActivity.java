package com.github.vatbub.hearingaid.profileeditor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileEditorActivity extends AppCompatActivity implements ProfileManager.ProfileManagerListener {
    private RecyclerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.profile_editor_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setAdapter(getAdapter());

        initButtonHandlers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ProfileManager.resetInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProfileManager.getInstance(this).getChangeListeners().add(this);
    }

    public RecyclerListAdapter getAdapter() {
        if (adapter == null)
            adapter = new RecyclerListAdapter(this, this.findViewById(R.id.profile_editor_recycler_view));
        return adapter;
    }

    private void initButtonHandlers() {
        FloatingActionButton floatingActionButton = findViewById(R.id.profile_editor_floatingActionButton);
        floatingActionButton.setOnClickListener(v -> ProfileManager.getInstance(ProfileEditorActivity.this).createProfile(getNameForNewProfile()));
    }

    private String getNameForNewProfile() {
        String nameTemplate = getString(R.string.profile_editor_new_profile_default_name);
        Pattern numberRegex = Pattern.compile("[0-9]+");
        Pattern nameRegex = Pattern.compile(nameTemplate.replace("%1$d", numberRegex.pattern()));
        List<ProfileManager.Profile> profiles = ProfileManager.getInstance(this).listProfiles();

        ArrayList<Long> numbers = new ArrayList<>(profiles.size());

        for (ProfileManager.Profile profile : profiles) {
            if (nameRegex.matcher(profile.getProfileName()).matches()) {
                Matcher matcher = numberRegex.matcher(profile.getProfileName());
                if (matcher.find())
                    numbers.add(Long.parseLong(matcher.group(0)));
            }
        }

        Collections.sort(numbers);


        long finalProfileNumber = numbers.get(numbers.size() - 1) + 1;
        long previousValue = -1;
        int skipCounter = 0;
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i) == previousValue) {
                skipCounter++;
                continue;
            }

            if (numbers.get(i) != i + 1 - skipCounter) {
                finalProfileNumber = i + 1 - skipCounter;
                break;
            }
            previousValue = numbers.get(i);
        }

        return String.format(nameTemplate, finalProfileNumber);
    }

    @Override
    public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {

    }

    @Override
    public void onProfileCreated(ProfileManager.Profile newProfile) {
        getAdapter().notifyItemInserted(ProfileManager.getInstance(this).getPosition(newProfile));
    }

    @Override
    public void onProfileDeleted(ProfileManager.Profile deletedProfile) {
        // handled by the adapter already
    }

    @Override
    public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {
        // handled by the adapter
    }
}
