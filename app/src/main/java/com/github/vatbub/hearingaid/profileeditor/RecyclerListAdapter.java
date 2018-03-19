package com.github.vatbub.hearingaid.profileeditor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;

import java.util.Collections;
import java.util.List;

/**
 * Created by frede on 23.02.2018.
 */

public class RecyclerListAdapter extends android.support.v7.widget.RecyclerView.Adapter<ProfileViewHolder> implements OnMoveAndSwipedListener {
    private Context callingContext;
    private View parentView;

    public RecyclerListAdapter(Context callingContext) {
        setCallingContext(callingContext);
    }

    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        parentView = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_editor_row, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        final ProfileManager.Profile profile = ProfileManager.getInstance(getCallingContext()).listProfiles().get(position);
        holder.getProfileNameTextView().setText(profile.getProfileName());
        holder.getProfileNameTextView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                profile.setProfileName(s.toString());
            }
        });
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

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        List<ProfileManager.Profile> profiles = ProfileManager.getInstance(getCallingContext()).listProfiles();
        Collections.swap(profiles, fromPosition, toPosition);
        ProfileManager.getInstance(getCallingContext()).setOrder(profiles);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(final int position) {
        // TODO: Add alert
        ProfileManager.Profile profile = ProfileManager.getInstance(getCallingContext()).listProfiles().get(position);
        final String profileName = profile.getProfileName();
        Snackbar.make(parentView, String.format(getCallingContext().getString(R.string.profile_editor_profile_deleted_snackbar), profileName), Snackbar.LENGTH_SHORT).show();
        ProfileManager.getInstance(getCallingContext()).deleteProfile(profile);
        notifyItemRemoved(position);
    }
}
