package com.github.vatbub.hearingaid.profileeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
    private RecyclerView parentView;
    private ItemTouchHelper itemTouchHelper;

    public RecyclerListAdapter(Context callingContext, RecyclerView parentView) {
        setCallingContext(callingContext);
        setParentView(parentView);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(this);
        setItemTouchHelper(new ItemTouchHelper(callback));
        getItemTouchHelper().attachToRecyclerView(getParentView());
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_editor_row, parent, false);
        return new ProfileViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ProfileViewHolder holder, int position) {
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
        holder.getDeleteButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteItemWithAlert(holder.getAdapterPosition());
            }
        });
        holder.getDragButton().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    getItemTouchHelper().startDrag(holder);
                return false;
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
    public void onItemDismiss(int position) {
        deleteItemWithAlert(position);
    }

    private void deleteItemWithAlert(final int position) {
        String profileName = ProfileManager.getInstance(getCallingContext()).listProfiles().get(position).getProfileName();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getCallingContext());

        // set title
        alertDialogBuilder.setTitle(R.string.profile_editor_delete_profile_alert_title);

        // set dialog message
        alertDialogBuilder
                .setMessage(String.format(getCallingContext().getString(R.string.profile_editor_delete_profile_alert_message), profileName))
                .setCancelable(true)
                .setPositiveButton(R.string.profile_editor_delete_profile_alert_button_delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteItem(position);
                    }
                })
                .setNegativeButton(R.string.profile_editor_delete_profile_button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        RecyclerListAdapter.this.notifyItemChanged(position);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void deleteItem(final int position) {
        final ProfileManager.Profile profile = ProfileManager.getInstance(getCallingContext()).listProfiles().get(position);

        final String profileName = profile.getProfileName();
        Snackbar.make(getParentView(), String.format(getCallingContext().getString(R.string.profile_editor_profile_deleted_snackbar), profileName), Snackbar.LENGTH_SHORT).show();
        ProfileManager.getInstance(getCallingContext()).deleteProfile(profile);
        notifyItemRemoved(position);
    }


    public ItemTouchHelper getItemTouchHelper() {
        return itemTouchHelper;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public RecyclerView getParentView() {
        return parentView;
    }

    public void setParentView(RecyclerView parentView) {
        this.parentView = parentView;
    }
}
