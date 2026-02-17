package com.github.vatbub.hearingaid.fragments;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.View;

/**
 * A Fragment that implements a {@code findById} method
 */

public abstract class CustomFragment extends Fragment {
    private View createdView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

    /**
     * Creates an instance for the specified tag.
     *
     * @param fragmentTag The tag of the fragment to instantiate
     * @return A new instance of the specified fragment
     */
    public static CustomFragment createInstance(FragmentTag fragmentTag) {
        switch (fragmentTag) {
            case ABOUT_FRAGMENT:
                return new AboutFragment();
            case PRIVACY_FRAGMENT:
                return new PrivacyFragment();
            case SETTINGS_FRAGMENT:
                return new SettingsFragment();
            case STREAMING_FRAGMENT:
                return new StreamingFragment();
        }

        return null;
    }

    public boolean onBackPressed() {
        return false;
    }

    public enum FragmentTag {
        ABOUT_FRAGMENT, PRIVACY_FRAGMENT, SETTINGS_FRAGMENT, STREAMING_FRAGMENT
    }
}
