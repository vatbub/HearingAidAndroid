package com.github.vatbub.hearingaid.profileeditor;

import android.support.v7.widget.RecyclerView;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.github.vatbub.hearingaid.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frede on 23.02.2018.
 */

public class ProfileViewHolder extends RecyclerView.ViewHolder {
    private EditText profileNameTextView;
    private ImageButton deleteButton;
    private ImageButton dragButton;
    private View itemView;
    private final List<TextWatcher> textWatchersForProfileNameTextBox = new ArrayList<>();

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

    public void addTextWatcherToProfileNameTextBox(TextWatcher textWatcher) {
        textWatchersForProfileNameTextBox.add(textWatcher);
        getProfileNameTextView().addTextChangedListener(textWatcher);
    }

    public void removeAllTextWatchersFromProfileNameTextView() {
        while (!textWatchersForProfileNameTextBox.isEmpty())
            getProfileNameTextView().removeTextChangedListener(textWatchersForProfileNameTextBox.remove(0));
    }
}
