package com.github.vatbub.hearingaid.profileeditor;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;

import java.util.List;

/**
 * Created by frede on 23.02.2018.
 */

public class ProfileViewHolder extends RecyclerView.ViewHolder implements ProfileManager.ProfileManagerListener {
    private EditText profileNameTextView;
    private ImageButton deleteButton;
    private ImageButton dragButton;
    private View itemView;
    private RecyclerListAdapter parentAdapter;

    public ProfileViewHolder(View itemView, RecyclerListAdapter parentAdapter, Context callingContext) {
        super(itemView);
        setParentAdapter(parentAdapter);
        setItemView(itemView);
        setDeleteButton(itemView.<ImageButton>findViewById(R.id.deleteButton));
        setDragButton(itemView.<ImageButton>findViewById(R.id.dragButton));
        setProfileNameTextView(itemView.<EditText>findViewById(R.id.profileNameTextView));

        ProfileManager.getInstance(callingContext).getChangeListeners().add(this);
        updateDeleteButtonEnabledFlag(0);
    }

    public EditText getProfileNameTextView() {
        return profileNameTextView;
    }

    public void setProfileNameTextView(EditText profileNameTextView) {
        this.profileNameTextView = profileNameTextView;
    }

    public ImageButton getDeleteButton() {
        return deleteButton;
    }

    public void setDeleteButton(ImageButton deleteButton) {
        this.deleteButton = deleteButton;
    }

    public ImageButton getDragButton() {
        return dragButton;
    }

    public void setDragButton(ImageButton dragButton) {
        this.dragButton = dragButton;
    }

    public View getItemView() {
        return itemView;
    }

    public void setItemView(View itemView) {
        this.itemView = itemView;
    }

    public RecyclerListAdapter getParentAdapter() {
        return parentAdapter;
    }

    public void setParentAdapter(RecyclerListAdapter parentAdapter) {
        this.parentAdapter = parentAdapter;
    }

    public void recycle(Context callingContext) {
        ProfileManager.getInstance(callingContext).getChangeListeners().remove(this);
    }

    @Override
    public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {

    }

    @Override
    public void onProfileCreated(ProfileManager.Profile newProfile) {
        updateDeleteButtonEnabledFlag(0);
    }

    @Override
    public void onProfileDeleted(ProfileManager.Profile deletedProfile) {
        updateDeleteButtonEnabledFlag(-1);
    }

    @Override
    public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {
        updateDeleteButtonEnabledFlag(0);
    }

    private void updateDeleteButtonEnabledFlag(int countCorrection) {
        int count = getParentAdapter().getItemCount() + countCorrection;
        System.out.println(count);
        getDeleteButton().setEnabled(count > 1);
    }
}
