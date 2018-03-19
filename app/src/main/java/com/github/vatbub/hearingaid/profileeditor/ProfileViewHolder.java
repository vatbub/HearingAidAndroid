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
    private ImageButton dragButton;

    public ProfileViewHolder(View itemView) {
        super(itemView);
        setDragButton(itemView.<ImageButton>findViewById(R.id.dragButton));
        setProfileNameTextView(itemView.<EditText>findViewById(R.id.profileNameTextView));
    }

    public EditText getProfileNameTextView() {
        return profileNameTextView;
    }

    public void setProfileNameTextView(EditText profileNameTextView) {
        this.profileNameTextView = profileNameTextView;
    }

    public ImageButton getDragButton() {
        return dragButton;
    }

    public void setDragButton(ImageButton dragButton) {
        this.dragButton = dragButton;
    }
}
