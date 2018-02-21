package com.github.vatbub.hearingaid.fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.github.vatbub.hearingaid.CrashlyticsManager;
import com.github.vatbub.hearingaid.FeedbackPrivacyActivity;
import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends CustomFragment implements ProfileManager.ActiveProfileChangeListener, AdapterView.OnItemSelectedListener {
    public static final int numberOfChannels = 6;
    private ArrayAdapter<ProfileManager.Profile> profileAdapter;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateEqSwitch();
        loadEqSettings();
        updateCrashlyticsCheckBox();

        initButtonHandlers();
        initFrequencyLabelsAndSeekbars();
    }

    private void updateCrashlyticsCheckBox() {
        AppCompatCheckBox crashReportsEnabledCheckBox = findViewById(R.id.enableCrashReportsCheckBox);
        crashReportsEnabledCheckBox.setChecked(CrashlyticsManager.getInstance(getContext()).isCrashlyticsEnabled());
    }

    @Override
    public void onResume() {
        super.onResume();
        ProfileManager.getInstance(getActivity()).getChangeListeners().add(this);
        initProfileSelector();
    }

    private void initProfileSelector() {
        Spinner profileSelector = findViewById(R.id.fragment_settings_profile_selector);
        profileSelector.setAdapter(getProfileAdapter());
        profileSelector.setOnItemSelectedListener(this);
        if (ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile() != null)
            profileSelector.setSelection(ProfileManager.getInstance(getContext()).getPosition(ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_settings_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fragment_settings_current_profile_add_button:
                addProfile();
                return true;
            case R.id.fragment_settings_current_profile_remove_button:
                removeProfile();
                return true;
            case R.id.fragment_settings_current_profile_rename_button:
                renameProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addProfile() {
        Context context = getContext();
        final MainActivity activity = (MainActivity) getActivity();
        if (context == null) {
            Crashlytics.logException(new NullPointerException("context was null"));
            return;
        }
        if (activity == null) {
            Crashlytics.logException(new NullPointerException("Activity was null"));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.fragment_settings_current_profile_add));

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.fragment_settings_add_profile_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileManager.Profile createdProfile = ProfileManager.getInstance(getActivity()).createProfile(input.getText().toString());
                activity.getProfileAdapter().add(createdProfile);
                getProfileAdapter().add(createdProfile);
                ProfileManager.getInstance(activity).applyProfile(createdProfile);
            }
        });
        builder.setNegativeButton(getString(R.string.fragment_settings_add_profile_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void renameProfile() {
        Context context = getContext();
        final MainActivity activity = (MainActivity) getActivity();
        final ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
        if (context == null) {
            Crashlytics.logException(new NullPointerException("context was null"));
            return;
        }
        if (activity == null) {
            Crashlytics.logException(new NullPointerException("Activity was null"));
            return;
        }
        if (currentProfile == null) {
            Crashlytics.logException(new NullPointerException("Current Profile was null"));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.fragment_settings_current_profile_rename));

        // Set up the input
        final EditText input = new EditText(getContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentProfile.getProfileName());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.fragment_settings_rename_profile_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentProfile.setProfileName(input.getText().toString());
                activity.initProfileAdapter();
                initProfileAdapter();
            }
        });
        builder.setNegativeButton(getString(R.string.fragment_settings_rename_profile_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void removeProfile() {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            Crashlytics.logException(new NullPointerException("Activity was null"));
            return;
        }

        ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
        ProfileManager.getInstance(getActivity()).deleteProfile(currentProfile);
        activity.getProfileAdapter().remove(currentProfile);
        getProfileAdapter().remove(currentProfile);
        ProfileManager.getInstance(getActivity()).applyProfile(ProfileManager.getInstance(getActivity()).listProfiles().get(0));
    }

    private void initButtonHandlers() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
                if (currentProfile != null)
                    currentProfile.setEqEnabled(checked);

                updateEQViewEnabledStatus(checked);

                StreamingFragment streamingFragment = (StreamingFragment) getActivity().getSupportFragmentManager().findFragmentByTag("streamingFragment");
                if (streamingFragment != null)
                    streamingFragment.notifyEQEnabledSettingChanged();
            }
        });

        AppCompatCheckBox crashReportsEnabledCheckBox = findViewById(R.id.enableCrashReportsCheckBox);
        crashReportsEnabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CrashlyticsManager.getInstance(getContext()).setCrashlyticsEnabled(isChecked);
                showRestartDialog();
            }
        });

        Button viewPrivacyStatementButton = findViewById(R.id.fragment_settings_view_privacy_button);
        viewPrivacyStatementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FeedbackPrivacyActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showRestartDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

        // set title
        alertDialogBuilder.setTitle(R.string.fragment_settings_restart_alert_title);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.fragment_settings_restart_alert_message)
                .setCancelable(true)
                .setPositiveButton(R.string.fragment_settings_restart_alert_restart_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        restartApp();
                    }
                })
                .setNegativeButton(R.string.fragment_settings_restart_alert_cancel_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void restartApp() {
        Context context = getContext();
        if (context == null) return;
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private void initFrequencyLabelsAndSeekbars() {
        double lowerFreq = FirebaseRemoteConfig.getInstance().getDouble(RemoteConfig.Keys.MIN_EQ_FREQUENCY);
        double higherFreq = FirebaseRemoteConfig.getInstance().getDouble(RemoteConfig.Keys.MAX_EQ_FREQUENCY);
        double hzPerChannel = (higherFreq - lowerFreq) / numberOfChannels;
        final int[] textViewIds = {R.id.text_view_bin_1, R.id.text_view_bin_2, R.id.text_view_bin_3, R.id.text_view_bin_4, R.id.text_view_bin_5, R.id.text_view_bin_6};

        for (int channel = 1; channel <= numberOfChannels; channel++) {
            double meanBinFreq = lowerFreq + (channel - 0.5) * hzPerChannel;
            // double higherBinFreq = lowerFreq + channel * hzPerChannel;

            String textToShow = getString(R.string.fragment_settings_frequency_bin_pattern_abbreviated).replace("{abbreviatedFrequency}", getStringForFrequency(meanBinFreq));

            ((TextView) findViewById(textViewIds[channel - 1])).setText(textToShow);

            VerticalSeekBar seekBar = findViewById(getSeekbarIDs()[channel - 1]);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        saveEqSettings();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    private void saveEqSettings() {
        List<Float> eqSettings = new ArrayList<>(getSeekbarIDs().length);
        for (int seekbarId : getSeekbarIDs()) {
            VerticalSeekBar seekBar = findViewById(seekbarId);
            eqSettings.add(seekBar.getProgress() / (float) seekBar.getMax());
        }

        ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile().setEQSettings(eqSettings);
    }

    private void loadEqSettings() {
        Context context = getContext();
        if (context == null) return;
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(context).getCurrentlyActiveProfile();
        if (currentProfile == null) return;
        List<Float> eqSettings = currentProfile.getEQSettings();
        int[] seekbarIDs = getSeekbarIDs();
        if (seekbarIDs.length != eqSettings.size()) {
            // setting was probably never set
            eqSettings = new ArrayList<>();
            for (int ignored : seekbarIDs) {
                eqSettings.add(0f);
            }
        }

        for (int i = 0; i < seekbarIDs.length; i++) {
            VerticalSeekBar seekBar = findViewById(seekbarIDs[i]);
            // seekBar.setProgress((int) (eqSettings.get(i) * seekBar.getMax()));
            animateSeekbar(seekBar, (int) (eqSettings.get(i) * seekBar.getMax()));
        }
    }

    private void animateSeekbar(final SeekBar seekBar, int toValue) {
        ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), toValue);
        anim.setDuration(400);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animProgress = (Integer) animation.getAnimatedValue();
                seekBar.setProgress(animProgress);
            }
        });
        anim.start();
    }

    private String getStringForFrequency(double frequency) {
        int mhzFactor = 1000000;
        int khzFactor = 1000;
        if (frequency >= mhzFactor) {
            return Long.toString(Math.round(frequency / mhzFactor)) + " " + getString(R.string.fragment_settings_MHz);
        } else if (frequency >= khzFactor) {
            return Long.toString(Math.round(frequency / khzFactor)) + " " + getString(R.string.fragment_settings_kHz);
        } else {
            return Long.toString(Math.round(frequency)) + " " + getString(R.string.fragment_settings_Hz);
        }
    }

    @Override
    public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
        if (newProfile == null)
            return;

        Spinner profileSelector = findViewById(R.id.fragment_settings_profile_selector);
        int position = ProfileManager.getInstance(getActivity()).getPosition(newProfile);
        profileSelector.setSelection(position);
        updateEqSwitch();
        loadEqSettings();
    }

    private void updateEqSwitch() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile();
        if (eqSwitch == null) return;
        if (currentProfile == null) return;
        eqSwitch.setChecked(currentProfile.isEqEnabled());
        updateEQViewEnabledStatus(currentProfile.isEqEnabled());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        ProfileManager.Profile selectedProfile = (ProfileManager.Profile) adapterView.getItemAtPosition(pos);
        ProfileManager.getInstance(getActivity()).applyProfile(selectedProfile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        System.out.println("Nothing selected");
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter() {
        if (profileAdapter == null) {
            profileAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_spinner_item_black);
            profileAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            initProfileAdapter();
        }
        return profileAdapter;
    }

    private void initProfileAdapter() {
        getProfileAdapter().clear();
        getProfileAdapter().addAll(ProfileManager.getInstance(getActivity()).listProfiles());
    }

    private int[] getSeekbarIDs() {
        return new int[]{R.id.eq_channel_1, R.id.eq_channel_2, R.id.eq_channel_3, R.id.eq_channel_4, R.id.eq_channel_5, R.id.eq_channel_6};
    }

    private void updateEQViewEnabledStatus(boolean enabled) {
        for (int seekbarId : getSeekbarIDs()) {
            findViewById(seekbarId).setEnabled(enabled);
        }
    }
}
