package com.github.vatbub.hearingaid.profileeditor;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.github.vatbub.hearingaid.R;

/**
 * Created by frede on 23.02.2018.
 */

public class ProfileViewHolder extends RecyclerView.ViewHolder {
    private EditText profileNameTextView;
    private ImageButton deleteButton;
    private ImageButton dragButton;
    private View itemView;

    public ProfileViewHolder(View itemView) {
        super(itemView);
        setItemView(itemView);
        setDeleteButton(itemView.findViewById(R.id.deleteButton));
        setDragButton(itemView.findViewById(R.id.dragButton));
        setProfileNameTextView(itemView.findViewById(R.id.profileNameTextView));
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
}
