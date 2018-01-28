package com.github.vatbub.hearingaid.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class SettingsFragment extends CustomFragment {
    public static final String SETTINGS_SHARED_PREFERENCES_NAME = "hearingAidSettings";
    public static final String EQ_ENABLED_PREF_KEY = "equalizerEnabled";
    public static final boolean EQ_ENABLED_DEFAULT_SETTING = true;
    public static final int numberOfChannels = 6;

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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        SharedPreferences prefs = getActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        eqSwitch.setChecked(prefs.getBoolean(EQ_ENABLED_PREF_KEY, EQ_ENABLED_DEFAULT_SETTING));

        initButtonHandlers();
        initFrequencyLabelsAndSeekbars();
    }

    private void initButtonHandlers() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences prefs = getActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(EQ_ENABLED_PREF_KEY, checked);
                editor.apply();

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
        int[] textViewIds = {R.id.text_view_bin_1, R.id.text_view_bin_2, R.id.text_view_bin_3, R.id.text_view_bin_4, R.id.text_view_bin_5, R.id.text_view_bin_6};

        for (int channel = 1; channel <= numberOfChannels; channel++) {
            double lowerBinFreq = lowerFreq + (channel - 1) * hzPerChannel;
            double higherBinFreq = lowerFreq + channel * hzPerChannel;
            System.out.println(lowerBinFreq);
            System.out.println(higherBinFreq);
            System.out.println(getString(R.string.fragment_settings_frequency_bin_pattern).replace("{lowerBinFrequency}", getStringForFrequency(lowerBinFreq)).replace("{higherBinFrequency}", getStringForFrequency(higherBinFreq)));

            ((TextView) findViewById(textViewIds[channel - 1])).setText(getString(R.string.fragment_settings_frequency_bin_pattern).replace("{lowerBinFrequency}", getStringForFrequency(lowerBinFreq)).replace("{higherBinFrequency}", getStringForFrequency(higherBinFreq)));
        }
    }

    private String getStringForFrequency(double frequency){
        int mhzFactor = 1000000;
        int khzFactor = 1000;
        if (frequency>=mhzFactor){
            return Long.toString(Math.round(frequency/mhzFactor)) + " " + getString(R.string.fragment_settings_MHz);
        }else if(frequency>=khzFactor){
            return Long.toString(Math.round(frequency/khzFactor)) + " " + getString(R.string.fragment_settings_kHz);
        }else{
            return Long.toString(Math.round(frequency)) + " " + getString(R.string.fragment_settings_Hz);
        }
    }
}
