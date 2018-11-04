package com.github.vatbub.hearingaid.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.R;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 */
public class AboutFragment extends CustomFragment {


    public AboutFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            MainActivity.displayMarkdown(getActivity(), R.raw.about, R.id.fragment_about_markdown_view);
        } catch (IOException e) {
            // TODO: Implement Bugsnag
        }
    }

}
