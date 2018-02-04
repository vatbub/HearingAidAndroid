package com.github.vatbub.hearingaid.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class SettingsFragment extends CustomFragment implements ProfileManager.ActiveProfileChangeListener, AdapterView.OnItemSelectedListener {
    public static final int numberOfChannels = 6;
    private ArrayAdapter<ProfileManager.Profile> profileAdapter;

    public SettingsFragment() {
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
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ProfileManager.getInstance(getActivity()).getChangeListeners().add(this);
        updateEqSwitch();

        initButtonHandlers();
        initFrequencyLabelsAndSeekbars();
        Spinner profileSelector = findViewById(R.id.fragment_settings_profile_selector);
        profileSelector.setAdapter(getProfileAdapter());
        profileSelector.setOnItemSelectedListener(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.fragment_settings_current_profile_add));

        // Set up the input
        final EditText input = new EditText(getContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.fragment_settings_add_profile_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileManager.Profile createdProfile = ProfileManager.getInstance(getActivity()).createProfile(input.getText().toString());
                ((MainActivity) getActivity()).getProfileAdapter().add(createdProfile);
                getProfileAdapter().add(createdProfile);
                ProfileManager.getInstance(getActivity()).applyProfile(createdProfile);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.fragment_settings_current_profile_rename));

        // Set up the input
        final EditText input = new EditText(getContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile().getProfileName());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.fragment_settings_rename_profile_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
                currentProfile.setProfileName(input.getText().toString());
                ((MainActivity) getActivity()).initProfileAdapter();
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
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
        ProfileManager.getInstance(getActivity()).deleteProfile(currentProfile);
        ((MainActivity) getActivity()).getProfileAdapter().remove(currentProfile);
        getProfileAdapter().remove(currentProfile);
        ProfileManager.getInstance(getActivity()).applyProfile(ProfileManager.getInstance(getActivity()).listProfiles().get(0));
    }

    private void initButtonHandlers() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile().setEqEnabled(checked);

                StreamingFragment streamingFragment = (StreamingFragment) getActivity().getFragmentManager().findFragmentByTag("streamingFragment");
                if (streamingFragment != null)
                    streamingFragment.notifyEQEnabledSettingChanged();
            }
        });
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
        }
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
    }

    private void updateEqSwitch() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setChecked(ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile().isEqEnabled());
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
}
