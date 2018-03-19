package com.github.vatbub.hearingaid.profileeditor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;

import java.util.List;

public class ProfileEditorActivity extends AppCompatActivity implements ProfileManager.ProfileManagerListener {
    private RecyclerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);
        ProfileManager.getInstance(this).getChangeListeners().add(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.profile_editor_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setAdapter(getAdapter());

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
        initButtonHandlers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ProfileManager.resetInstance(this);
    }

    public RecyclerListAdapter getAdapter() {
        if (adapter == null)
            adapter = new RecyclerListAdapter(this);
        return adapter;
    }

    private void initButtonHandlers() {
        FloatingActionButton floatingActionButton = findViewById(R.id.profile_editor_floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileManager.getInstance(ProfileEditorActivity.this).createProfile("");
            }
        });
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
