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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Spinner;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.Toast;
import android.widget.ImageButton;

import com.github.vatbub.hearingaid.BottomSheetQueue;
import com.github.vatbub.hearingaid.Constants;
import com.github.vatbub.hearingaid.CustomApplication;
import com.github.vatbub.hearingaid.FeedbackPrivacyActivity;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.github.vatbub.hearingaid.backend.HearingAidPlaybackService;

import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class StreamingFragment extends CustomFragment implements ProfileManager.ProfileManagerListener {
    private static final String SHARED_PREFERENCES_FILE_NAME = "com.github.vatbub.hearingaid.fragments.StreamingFragment.Preferences";
    private static final String NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY = "doNotShowLowLatencyMessage";
    private static final String NEVER_SHOW_WIRED_HEADPHONES_MESSAGE_AGAIN_PREF_KEY = "doNotShowWiredHeadphonesMessage";

    private BottomSheetBehavior mWiredHeadphonesBottomSheetBehavior;
    private BottomSheetBehavior mDiagnosticDataBottomSheetBehavior;
    private BottomSheetBehavior mLatencyBottomSheetBehavior;
    
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private BottomSheetQueue bottomSheetBehaviourQueue;

    private MediaBrowserCompat mMediaBrowser;
    private ImageButton mPlayPause;
    private MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    boolean isStreaming = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                    updateButtonUI(isStreaming);
                    if (isStreaming) {
                        Snackbar.make(view.findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_start_streaming, 3000).show();
                    } else {
                        Snackbar.make(view.findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_stop_streaming, 3000).show();
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
                        e.printStackTrace();
                        // Removed Bugsnag usage
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
    private View view;

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public boolean onBackPressed() {
        if (bottomSheetBehaviourQueue == null)
            return super.onBackPressed();

        return bottomSheetBehaviourQueue.handleBackPress();
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
        updateButtonUI(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback);
    }

    private void updateButtonUI(boolean isStreaming) {
        if (mPlayPause == null) return;

        TextView label = view.findViewById(R.id.streaming_status_label);
        
        if (isStreaming) {
            mPlayPause.setBackgroundResource(R.drawable.atlas_circle_active);
            mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            if (label != null) label.setText("Listening...");
        } else {
            // Keep it colored but maybe different icon or state
             mPlayPause.setBackgroundResource(R.drawable.atlas_circle_active); 
             mPlayPause.setImageResource(android.R.drawable.ic_btn_speak_now);
             if (label != null) label.setText("Paused");
        }
    }

    public boolean isStreamingEnabled() {
        int playbackState = MediaControllerCompat.getMediaController(getActivity()).getPlaybackState().getState();
        return playbackState == PlaybackStateCompat.STATE_PLAYING;
    }

    private void initProfileSelector() {
        Spinner spinner = view.findViewById(R.id.dashboard_profile_selector);
        final ProfileManager pm = ProfileManager.getInstance(getContext());
        final List<ProfileManager.Profile> profiles = pm.listProfiles();
        final List<String> profileNames = new java.util.ArrayList<>();
        
        for (ProfileManager.Profile p : profiles) {
            profileNames.add(p.getProfileName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, profileNames);
        spinner.setAdapter(adapter);
        
        ProfileManager.Profile activeProfile = pm.getCurrentlyActiveProfile();
        if (activeProfile != null) {
            int idx = -1;
            for (int i = 0; i < profiles.size(); i++) {
                if (profiles.get(i).equals(activeProfile)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) spinner.setSelection(idx);
        }
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 if (position < 0 || position >= profiles.size()) return;
                 
                 ProfileManager.Profile selectedProfile = profiles.get(position);
                 ProfileManager.Profile current = pm.getCurrentlyActiveProfile();
                 
                 if (current == null || !current.equals(selectedProfile)) {
                     pm.applyProfile(selectedProfile);
                 }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_streaming, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Grab the view for the play/pause button
        mPlayPause = view.findViewById(R.id.mainToggleButton);

        initButtonHandlers();
        initProfileSelector();

        // Custom Drawer Toggle
        View toggle = view.findViewById(R.id.custom_drawer_toggle);
        if (toggle != null) {
            toggle.setOnClickListener(v -> {
                DrawerLayout drawer = getActivity().findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    drawer.openDrawer(GravityCompat.START);
                }
            });
        }

        // Initialize Volume Slider
        android.widget.SeekBar volumeSlider = view.findViewById(R.id.volume_slider);
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSlider.setMax(maxVol);
        volumeSlider.setProgress(currentVol);
        TextView volumeText = view.findViewById(R.id.volume_text);
        volumeText.setText((int)((currentVol / (float)maxVol) * 100) + "%");

        volumeSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                volumeText.setText((int)((progress / (float)maxVol) * 100) + "%");
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        View diagnosticDataBottomSheet = view.findViewById(R.id.diagnostic_data_bottom_Sheet);
        mDiagnosticDataBottomSheetBehavior = BottomSheetBehavior.from(diagnosticDataBottomSheet);
        mDiagnosticDataBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        View lowLatencyBottomSheet = view.findViewById(R.id.low_latency_bottom_sheet);
        mLatencyBottomSheetBehavior = BottomSheetBehavior.from(lowLatencyBottomSheet);
        mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        View wiredHeadphonesBottomSheet = view.findViewById(R.id.connect_headphones_bottom_sheet);
        mWiredHeadphonesBottomSheetBehavior = BottomSheetBehavior.from(wiredHeadphonesBottomSheet);
        mWiredHeadphonesBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        showLatencyBottomSheetIfApplicable();
        showDiagnosticDataBottomSheetIfApplicable();
    }

    private void showDiagnosticDataBottomSheetIfApplicable() {
        // Disabled Bugsnag check
        // if (CustomApplication.hasUserMadeAChoiceForBugsnag(getContext())) return;
        // bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mDiagnosticDataBottomSheetBehavior, false, BottomSheetBehavior.STATE_EXPANDED, BottomSheetQueue.BottomSheetPriority.HIGHER_THAN_NORMAL));
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

            ((TextView) view.findViewById(R.id.tv_low_latency)).setText(getString(R.string.fragment_streaming_latency_message, comparisonString));
            bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mLatencyBottomSheetBehavior, false, BottomSheetBehavior.STATE_EXPANDED, BottomSheetQueue.BottomSheetPriority.LOW));
        }
    }

    public void initButtonHandlers() {
        mPlayPause.setOnClickListener(v -> {
            if (permissionMissing()) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                setStreaming(!isStreamingEnabled());
            }
        });

        view.findViewById(R.id.diagnostic_data_yes_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.fragment_streaming_diagnostic_data_change_later_in_settings_toast, Toast.LENGTH_LONG).show();
            // CustomApplication.setBugSnagEnabled(getContext(), true);
            mDiagnosticDataBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });

        view.findViewById(R.id.diagnostic_data_no_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.fragment_streaming_diagnostic_data_change_later_in_settings_toast, Toast.LENGTH_LONG).show();
            // CustomApplication.setBugSnagEnabled(getContext(), false);
            mDiagnosticDataBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });

        view.findViewById(R.id.diagnostic_data_privacy_policy_button).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), FeedbackPrivacyActivity.class));
        });

        view.findViewById(R.id.learn_more_button).setOnClickListener(v -> {
            mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            String url = RemoteConfig.getConfig().getValue(RemoteConfig.Keys.LATENCY_MORE_INFO_URL);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        view.findViewById(R.id.low_latency_dont_show_again_button).setOnClickListener(v -> {
            mLatencyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putBoolean(NEVER_SHOW_LOW_LATENCY_MESSAGE_AGAIN_PREF_KEY, true);
            editor.apply();
        });

        view.findViewById(R.id.wired_headphones_play_anyway_button).setOnClickListener(v -> {
            mWiredHeadphonesBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            setStreaming(true, true);
        });

        view.findViewById(R.id.wired_headphones_dont_show_again_button).setOnClickListener(v -> {
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
            bottomSheetBehaviourQueue.add(new BottomSheetQueue.BottomSheetBehaviourWrapper(mWiredHeadphonesBottomSheetBehavior, true, BottomSheetBehavior.STATE_EXPANDED, BottomSheetQueue.BottomSheetPriority.HIGH));
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
