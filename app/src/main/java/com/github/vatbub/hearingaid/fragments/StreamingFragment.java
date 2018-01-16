package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.github.vatbub.common.view.motd.PlatformIndependentMOTD;
import com.github.vatbub.hearingaid.AndroidMOTDFileOutputStreamProvider;
import com.github.vatbub.hearingaid.BottomSheetQueue;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.ohoussein.playpause.PlayPauseView;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.io.FeedException;

import java.io.IOException;
import java.net.URL;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.github.vatbub.hearingaid.fragments.SettingsFragment.EQ_ENABLED_DEFAULT_SETTING;
import static com.github.vatbub.hearingaid.fragments.SettingsFragment.EQ_ENABLED_PREF_KEY;
import static com.github.vatbub.hearingaid.fragments.SettingsFragment.SETTINGS_SHARED_PREFERENCES_NAME;

public class StreamingFragment extends CustomFragment {
    private static final String SUPERPOWERED_INITIALIZED_BUNDLE_KEY = "superpoweredInitialized";
    private static final String IS_STREAMING_BUNDLE_KEY = "isStreaming";
    private static final String SHARED_PREFERENCES_FILE_NAME = "com.github.vatbub.hearingaid.fragments.StreamingFragment.Preferences";
    private static final String NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY = "doNotShowLowLatencyMessage";

    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private boolean isStreaming;
    private boolean superpoweredInitialized = false;
    private BottomSheetBehavior mLatencyBottomSheetBehavior;
    private BottomSheetBehavior mMOTDBottomSheetBehavior;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private BottomSheetQueue bottomSheetBehaviourQueue;

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length == 0 || grantResults.length == 0 || grantResults[0] == PERMISSION_DENIED) {
            setStreaming(false);
            ((PlayPauseView) findViewById(R.id.mainToggleButton)).change(!isStreamingEnabled());
            return;
        }

        updateStreamingState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (superpoweredInitialized && isStreamingEnabled())
            onForeground();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (superpoweredInitialized)
            onBackground();
    }

    public boolean isStreamingEnabled() {
        return isStreaming;
    }

    private void updateStreamingState() {
        initSuperpoweredIfNotInitialized();
        if (isStreamingEnabled()) {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_start_streaming, 3000).show();
        } else {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_stop_streaming, 3000).show();
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

        if (savedInstanceState != null) {
            superpoweredInitialized = savedInstanceState.getBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY);
            setStreaming(savedInstanceState.getBoolean(IS_STREAMING_BUNDLE_KEY));
        }

        bottomSheetBehaviourQueue = new BottomSheetQueue();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY, superpoweredInitialized);
        outState.putBoolean(IS_STREAMING_BUNDLE_KEY, isStreamingEnabled());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initButtonHandlers();

        View lowLatencyBottomSheet = findViewById(R.id.low_latency_bottom_sheet);
        mLatencyBottomSheetBehavior = BottomSheetBehavior.from(lowLatencyBottomSheet);
        mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        View motdBottomSheet = findViewById(R.id.motd_bottom_sheet);
        mMOTDBottomSheetBehavior = BottomSheetBehavior.from(motdBottomSheet);
        mMOTDBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mMOTDBottomSheetBehavior.setPeekHeight(790);

        showMOTDIfApplicable();
        showLatencyBottomSheetIfApplicable();
    }

    private SharedPreferences getSharedPreferences() {
        return getActivity().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    private void showLatencyBottomSheetIfApplicable() {
        if (getSharedPreferences().getBoolean(NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY, false))
            return;

        boolean hasLowLatencyFeature =
                getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        boolean hasProFeature =
                getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);

        if (!hasLowLatencyFeature || !hasProFeature) {

            String comparisonString;
            if (hasLowLatencyFeature)
                comparisonString = getString(R.string.fragment_streaming_up_to);
            else
                comparisonString = getString(R.string.fragment_streaming_more_than);

            ((TextView) findViewById(R.id.tv_low_latency)).setText(getString(R.string.fragment_streaming_latency_message, comparisonString));
            bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mLatencyBottomSheetBehavior));
        }
    }

    public void initButtonHandlers() {
        findViewById(R.id.mainToggleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStreaming(!isStreamingEnabled());
                ((PlayPauseView) v).change(!isStreamingEnabled());
                if (!allPermissionsGranted()) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    updateStreamingState();
                }
            }
        });

        findViewById(R.id.learn_more_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                String url = FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.LATENCY_MORE_INFO_URL);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        findViewById(R.id.dont_show_again_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                SharedPreferences.Editor editor = getSharedPreferences().edit();
                editor.putBoolean(NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY, true);
                editor.apply();
            }
        });
    }

    /**
     * Initializes the superpowered sdk and associated c++ code.
     * No-op if already initialized.
     */
    private void initSuperpoweredIfNotInitialized() {
        if (superpoweredInitialized)
            return;

        // Get the device's sample rate and buffer size to enable low-latency Android audio io, if available.
        String samplerateString, buffersizeString;
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        HearingAidAudioProcessor(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString));

        notifyEQEnabledSettingChanged();

        superpoweredInitialized = true;
    }

    private native void HearingAidAudioProcessor(int samplerate, int buffersize);

    private native void onPlayPause(boolean play);

    private native void onBackground();

    private native void onForeground();

    private native void eqEnabled(boolean eqEnabled);

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    private void showMOTDIfApplicable() {
        // Show messages of the day
        Thread motdThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final PlatformIndependentMOTD motd;
                try {
                    PlatformIndependentMOTD.setMotdFileOutputStreamProvider(new AndroidMOTDFileOutputStreamProvider(getActivity()));
                    motd = PlatformIndependentMOTD.getLatestMOTD(new URL(FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.MOTD_URL)));
                    if (!motd.isMarkedAsRead()) {
                        // Get the motd content
                        StringBuilder content = new StringBuilder("<head><style>" + FirebaseRemoteConfig.getInstance().getString(RemoteConfig.Keys.MOTD_CSS) + "</style></head><body><div class=\"motdContent\" id=\"motdContent\">");
                        content.append("<p><h3>").append(motd.getEntry().getTitle()).append("</h3></p>");
                        for (SyndContent str : motd.getEntry().getContents()) {
                            if (str.getValue() != null) {
                                content.append(str.getValue());
                            }
                        }
                        content.append("</div></body>");

                        if (content.toString().contains("<span id=\"more")) {
                            // We've got a read more link so stop parsing the message
                            // and change the button caption to imply that there is more
                            // to read
                            content = new StringBuilder(content.substring(0, content.indexOf("<span id=\"more")));
                            // openWebpageButton.setText(bundle.getString("readMoreLink"));
                        }

                        final String finalContent = content.toString();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                WebView motdView = findViewById(R.id.motd_web_view);
                                motdView.loadData(finalContent, "text/html", "UTF-8");
                            }
                        });

                        BottomSheetQueue.BottomSheetCallbackList additionalCallbacks = new BottomSheetQueue.BottomSheetCallbackList();
                        additionalCallbacks.add(new BottomSheetBehavior.BottomSheetCallback() {
                            @Override
                            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                                    try {
                                        motd.markAsRead();
                                    } catch (IOException | ClassNotFoundException e) {
                                        FirebaseCrash.report(e);
                                    }
                                }
                            }

                            @Override
                            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                            }
                        });

                        Button readMoreButton = findViewById(R.id.motd_read_more);
                        readMoreButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mMOTDBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(motd.getEntry().getUri()));
                                startActivity(intent);
                            }
                        });

                        bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mMOTDBottomSheetBehavior, BottomSheetBehavior.STATE_COLLAPSED, additionalCallbacks));
                    }
                } catch (IllegalArgumentException | FeedException | IOException | ClassNotFoundException e) {
                    FirebaseCrash.report(e);
                }
            }
        });
        motdThread.setName("motdThread");
        motdThread.start();
    }

    public void notifyEQEnabledSettingChanged() {
        if (!superpoweredInitialized)
            return;

        SharedPreferences prefs = getActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        eqEnabled(prefs.getBoolean(EQ_ENABLED_PREF_KEY, EQ_ENABLED_DEFAULT_SETTING));
    }
}
