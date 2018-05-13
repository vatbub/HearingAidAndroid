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

import java.util.List;
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
        Pattern nameRegex = Pattern.compile(nameTemplate.replace("%1$d", "[0-9]*"));
        List<ProfileManager.Profile> profiles = ProfileManager.getInstance(this).listProfiles();
        int counter = 1;

        for (ProfileManager.Profile profile : profiles) {
            if (nameRegex.matcher(profile.getProfileName()).matches())
                counter++;
        }
        return String.format(nameTemplate, counter);
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
