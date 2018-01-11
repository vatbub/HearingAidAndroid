package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
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

import com.github.vatbub.hearingaid.R;

public class StreamingFragment extends Fragment {
    private static final String SUPERPOWERED_INITIALIZED_BUNDLE_KEY = "superpoweredInitialized";

    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private View createdView;
    private boolean superpoweredInitialized = false;

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        updateStreamingState();
    }

    public boolean isStreamingEnabled() {
        return ((ToggleButton) findViewById(R.id.mainToggleButton)).isChecked();
    }

    private void updateStreamingState() {
        initSuperpoweredIfNotInitialized();
        if (isStreamingEnabled()) {
            Snackbar.make(findViewById(R.id.fragment_content), "Started streaming", 3000).show();
        } else {
            Snackbar.make(findViewById(R.id.fragment_content), "Stopped streaming", 3000).show();
        }
        onPlayPause(isStreamingEnabled());
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean allPermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return false;

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            superpoweredInitialized = savedInstanceState.getBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY, superpoweredInitialized);
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
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    updateStreamingState();
                }
            }
        });

    }

    private <T extends View> T findViewById(@IdRes int id) {
        return createdView.findViewById(id);
    }

    /**
     * Initializes the superpowered sdk and associated c++ code.
     * No-op if already initialized.
     */
    private void initSuperpoweredIfNotInitialized() {
        if (superpoweredInitialized)
            return;

        // Get the device's sample rate and buffer size to enable low-latency Android audio io, if available.
        String samplerateString = null, buffersizeString = null;
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        HearingAidAudioProcessor(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString));

        superpoweredInitialized = true;
    }

    private native void HearingAidAudioProcessor(int samplerate, int buffersize);

    private native void onPlayPause(boolean play);
}
