package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.github.vatbub.hearingaid.MediaStreamer;
import com.github.vatbub.hearingaid.R;

public class StreamingFragment extends Fragment {
    private View createdView;

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        updateStreamingState();
    }

    public boolean isStreamingEnabled() {
        return ((ToggleButton) findViewById(R.id.mainToggleButton)).isChecked();
    }

    private void updateStreamingState() {
        if (isStreamingEnabled()) {
            Snackbar.make(findViewById(R.id.fragment_content), "Started streaming", 3000).show();
            MediaStreamer.startStreaming();
        } else {
            Snackbar.make(findViewById(R.id.fragment_content), "Stopped streaming", 3000).show();
            MediaStreamer.stopStreaming();
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean allPermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return false;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return false;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return false;

        return true;
    }

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        createdView = view;

        findViewById(R.id.mainToggleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!allPermissionsGranted()) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                } else {
                    updateStreamingState();
                }
            }
        });
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return createdView.findViewById(id);
    }
}
