package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.github.vatbub.common.view.motd.PlatformIndependentMOTD;
import com.github.vatbub.hearingaid.AndroidMOTDFileOutputStreamProvider;
import com.github.vatbub.hearingaid.BottomSheetQueue;
import com.github.vatbub.hearingaid.Constants;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.github.vatbub.hearingaid.backend.HearingAidPlaybackService;
import com.ohoussein.playpause.PlayPauseView;
import com.rometools.rome.feed.synd.SyndContent;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class StreamingFragment extends CustomFragment implements ProfileManager.ProfileManagerListener {
    private static final String SHARED_PREFERENCES_FILE_NAME = "com.github.vatbub.hearingaid.fragments.StreamingFragment.Preferences";
    private static final String NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY = "doNotShowLowLatencyMessage";
    private static final String NEVER_SHOW_WIRED_HEADPHONES_MESSAGE_AGAIN_PREF_KEY = "doNotShowWiredHeadphonesMessage";

    private BottomSheetBehavior mWiredHeadphonesBottomSheetBehavior;
    private BottomSheetBehavior mLatencyBottomSheetBehavior;
    private BottomSheetBehavior mMOTDBottomSheetBehavior;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private BottomSheetQueue bottomSheetBehaviourQueue;

    private MediaBrowserCompat mMediaBrowser;
    private PlayPauseView mPlayPause;
    private MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    boolean isStreaming = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                    mPlayPause.change(!isStreaming);
                    if (isStreaming) {
                        Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_start_streaming, 3000).show();
                    } else {
                        Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_stop_streaming, 3000).show();
                    }
                }
            };
    private boolean startStreamAfterConnectingToMediaBrowserService;
    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        // Get the token for the MediaSession
                        MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

                        MediaControllerCompat mediaController = new MediaControllerCompat(getContext(), // Context
                                token);

                        // Save the controller
                        MediaControllerCompat.setMediaController(getActivity(), mediaController);

                        // Finish building the UI
                        buildTransportControls();

                        if (startStreamAfterConnectingToMediaBrowserService)
                            setStreaming(true);
                    } catch (RemoteException e) {
                        // TODO: Implement Bugsnag
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaControllerCompat mediaControllerCompat = MediaControllerCompat.getMediaController(getActivity());
        if (mediaControllerCompat != null)
            mediaControllerCompat.getTransportControls().stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length == 0 || grantResults.length == 0 || grantResults[0] == PERMISSION_DENIED) {
            setStreaming(false);
            return;
        }

        // permission granted
        connectMediaBrowser();
        startStreamAfterConnectingToMediaBrowserService = true;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean permissionMissing() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return true;

        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bottomSheetBehaviourQueue = new BottomSheetQueue();
        ProfileManager.getInstance(getActivity()).getChangeListeners().add(this);

        initMediaBrowser();
    }

    private void initMediaBrowser() {
        if (permissionMissing()) return;
        if (getContext() == null) return;
        if (mMediaBrowser != null) return;

        mMediaBrowser = new MediaBrowserCompat(getContext(),
                new ComponentName(getContext(), HearingAidPlaybackService.class),
                mConnectionCallbacks,
                null); // optional Bundle
    }

    private void connectMediaBrowser() {
        if (permissionMissing()) return;
        initMediaBrowser();

        mMediaBrowser.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
        connectMediaBrowser();
    }

    @Override
    public void onStop() {
        super.onStop();

        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(getActivity()) != null) {
            MediaControllerCompat.getMediaController(getActivity()).unregisterCallback(controllerCallback);
        }
        if (mMediaBrowser != null)
            mMediaBrowser.disconnect();
    }

    private void buildTransportControls() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());

        // Display the initial state
        PlaybackStateCompat playbackState = mediaController.getPlaybackState();
        mPlayPause.change(playbackState.getState() != PlaybackStateCompat.STATE_PLAYING, false);

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback);
    }

    public boolean isStreamingEnabled() {
        int playbackState = MediaControllerCompat.getMediaController(getActivity()).getPlaybackState().getState();
        return playbackState == PlaybackStateCompat.STATE_PLAYING;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Grab the view for the play/pause button
        mPlayPause = findViewById(R.id.mainToggleButton);

        initButtonHandlers();

        View lowLatencyBottomSheet = findViewById(R.id.low_latency_bottom_sheet);
        mLatencyBottomSheetBehavior = BottomSheetBehavior.from(lowLatencyBottomSheet);
        mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        View wiredHeadphonesBottomSheet = findViewById(R.id.connect_headphones_bottom_sheet);
        mWiredHeadphonesBottomSheetBehavior = BottomSheetBehavior.from(wiredHeadphonesBottomSheet);
        mWiredHeadphonesBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

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
                false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasProFeature = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        }

        if (!hasLowLatencyFeature || !hasProFeature) {

            String comparisonString;
            if (hasLowLatencyFeature)
                comparisonString = getString(R.string.fragment_streaming_up_to);
            else
                comparisonString = getString(R.string.fragment_streaming_more_than);

            ((TextView) findViewById(R.id.tv_low_latency)).setText(getString(R.string.fragment_streaming_latency_message, comparisonString));
            bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mLatencyBottomSheetBehavior, BottomSheetBehavior.STATE_EXPANDED, BottomSheetQueue.BottomSheetPriority.LOW));
        }
    }

    public void initButtonHandlers() {
        mPlayPause.setOnClickListener(view -> {
            if (permissionMissing()) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                setStreaming(!isStreamingEnabled());
            }
        });

        findViewById(R.id.learn_more_button).setOnClickListener(view -> {
            mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            String url = RemoteConfig.getConfig().getValue(RemoteConfig.Keys.LATENCY_MORE_INFO_URL);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        findViewById(R.id.low_latency_dont_show_again_button).setOnClickListener(view -> {
            mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putBoolean(NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY, true);
            editor.apply();
        });

        findViewById(R.id.wired_headphones_play_anyway_button).setOnClickListener(view -> {
            mWiredHeadphonesBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            setStreaming(true, true);
        });

        findViewById(R.id.wired_headphones_dont_show_again_button).setOnClickListener(view -> {
            mWiredHeadphonesBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putBoolean(NEVER_SHOW_WIRED_HEADPHONES_MESSAGE_AGAIN_PREF_KEY, true);
            editor.apply();
        });
    }

    private boolean isConnectedViaBluetooth() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true;
                }
            }
            return false;
        } else {
            return audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn();
        }
    }

    public void setStreaming(boolean streaming) {
        setStreaming(streaming, false);
    }

    public void setStreaming(boolean streaming, boolean forceStreaming) {
        if (streaming) {
            if (forceStreaming || getSharedPreferences().getBoolean(NEVER_SHOW_WIRED_HEADPHONES_MESSAGE_AGAIN_PREF_KEY, false) || isHeadphonesPlugged()) {
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().play();
                return;
            }
            bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mWiredHeadphonesBottomSheetBehavior, BottomSheetBehavior.STATE_EXPANDED, BottomSheetQueue.BottomSheetPriority.HIGH));
        } else {
            MediaControllerCompat.getMediaController(getActivity()).getTransportControls().pause();
        }
    }

    private boolean isHeadphonesPlugged() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    return true;
                }
            }
            return false;
        } else {
            return audioManager.isWiredHeadsetOn();
        }
    }

    private void showMOTDIfApplicable() {
        // Show messages of the day
        Thread motdThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final PlatformIndependentMOTD motd;
                try {
                    PlatformIndependentMOTD.setMotdFileOutputStreamProvider(new AndroidMOTDFileOutputStreamProvider(getActivity()));
                    motd = PlatformIndependentMOTD.getLatestMOTD(new URL(RemoteConfig.getConfig().getValue(RemoteConfig.Keys.MOTD_URL)));
                    if (motd == null) return;
                    if (motd.isMarkedAsRead()) return;

                    // Get the motd content
                    StringBuilder content = new StringBuilder("<head><style>" + RemoteConfig.getConfig().getValue(RemoteConfig.Keys.MOTD_CSS) + "</style></head><body><div class=\"motdContent\" id=\"motdContent\">");
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

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                WebView motdView = findViewById(R.id.motd_web_view);

                                final BottomSheetQueue.BottomSheetCallbackList additionalCallbacks = new BottomSheetQueue.BottomSheetCallbackList();
                                additionalCallbacks.add(new BottomSheetQueue.CustomBottomSheetCallback() {
                                    @Override
                                    public void onRescheduled() {

                                    }

                                    @Override
                                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                                            try {
                                                motd.markAsRead();
                                            } catch (IOException | ClassNotFoundException e) {
                                                // TODO: Implement Bugsnag
                                            }
                                        }
                                    }

                                    @Override
                                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                                    }
                                });

                                motdView.setWebViewClient(new WebViewClient() {
                                    @Override
                                    public void onPageFinished(WebView view, String url) {
                                        super.onPageFinished(view, url);
                                        bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mMOTDBottomSheetBehavior, BottomSheetBehavior.STATE_COLLAPSED, BottomSheetQueue.BottomSheetPriority.NORMAL, additionalCallbacks));
                                    }
                                });

                                motdView.loadData(finalContent, "text/html", "UTF-8");
                            }
                        });
                    }

                    Button readMoreButton = findViewById(R.id.motd_read_more);
                    readMoreButton.setOnClickListener(view -> {
                        mMOTDBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(motd.getEntry().getUri()));
                        startActivity(intent);
                    });

                    Button closeButton = findViewById(R.id.motd_close);
                    closeButton.setOnClickListener(view -> {
                        mMOTDBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO: Implement Bugsnag
                }
            }
        });
        motdThread.setName("motdThread");
        motdThread.start();
    }

    public void notifyEQEnabledSettingChanged() {
        if (getContext() == null) return;
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile();
        if (currentProfile == null) return;
        Activity activity = getActivity();
        if (activity == null) return;
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(activity);
        if (mediaController == null) return;

        Bundle params = new Bundle();
        params.putBoolean(Constants.EQ_CHANGED_RESULT, currentProfile.isEqEnabled());

        mediaController.sendCommand(Constants.CUSTOM_COMMAND_NOTIFY_EQ_ENABLED_CHANGED, params, null);
    }

    @Override
    public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
        notifyEQEnabledSettingChanged();
    }

    @Override
    public void onProfileCreated(ProfileManager.Profile newProfile) {
        // no op
    }

    @Override
    public void onProfileDeleted(ProfileManager.Profile deletedProfile) {
        // no op
    }

    @Override
    public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {
        // no op
    }

    private class HearingAidConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
    }
}
