package com.github.vatbub.hearingaid.profileeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;

/**
 * Created by frede on 23.02.2018.
 */

public class RecyclerListAdapter extends android.support.v7.widget.RecyclerView.Adapter<ProfileViewHolder> {
    private Context callingContext;

    public RecyclerListAdapter(Context callingContext) {
        setCallingContext(callingContext);
    }

    @Override
    public ProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_editor_row, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProfileViewHolder holder, int position) {
        ProfileManager.Profile profile = ProfileManager.getInstance(getCallingContext()).listProfiles().get(position);
        holder.getProfileNameTextView().setText(profile.getProfileName());
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return ProfileManager.getInstance(getCallingContext()).listProfiles().size();
    }

    public Context getCallingContext() {
        return callingContext;
    }

    public void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }
}
