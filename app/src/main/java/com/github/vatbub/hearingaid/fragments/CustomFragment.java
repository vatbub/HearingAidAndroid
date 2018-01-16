package com.github.vatbub.hearingaid.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * A Fragment that implements a {@code findById} method
 */

public class CustomFragment extends Fragment {
    private View createdView;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createdView = view;
    }

    /**
     * This method works just like {@link android.app.Activity#findViewById(int)}. <b>Note:</b> This method only works <i>after</i> {@link #onViewCreated(View, Bundle)} has been called. That means:
     * <ul>
     *     <li>Do not call this method in {@link #onCreate(Bundle)}</li>
     *     <li>If you override {@link #onViewCreated(View, Bundle)}, you <i>must</i> call {@code super.onCreated(view, savedInstanceState)}</li>
     *     <li>You may call this method in {@link #onViewCreated(View, Bundle)} <i>after</i> you called {@code super.onCreated(view, savedInstanceState)}</li>
     * </ul>
     * @param id The id of the view to get
     * @param <T> The View class
     * @return The requested view object
     */
    public <T extends View> T findViewById(@IdRes int id) {
        return createdView.findViewById(id);
    }
}
