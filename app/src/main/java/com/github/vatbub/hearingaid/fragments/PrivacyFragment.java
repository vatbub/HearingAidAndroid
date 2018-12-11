package com.github.vatbub.hearingaid.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.vatbub.hearingaid.BugsnagWrapper;
import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.R;

import java.io.IOException;

public class PrivacyFragment extends CustomFragment {

    public PrivacyFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            MainActivity.displayMarkdown(getActivity(), R.raw.privacy, R.id.fragment_privacy_markdown_view);
        } catch (IOException e) {
            e.printStackTrace();
            BugsnagWrapper.notify(e);
        }
    }
}
