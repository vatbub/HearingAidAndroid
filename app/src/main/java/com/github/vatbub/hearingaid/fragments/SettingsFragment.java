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

import com.github.vatbub.hearingaid.R;

public class SettingsFragment extends CustomFragment {
    public static final String SETTINGS_SHARED_PREFERENCES_NAME = "hearingAidSettings";
    public static final String EQ_ENABLED_PREF_KEY = "equalizerEnabled";
    public static final boolean  EQ_ENABLED_DEFAULT_SETTING = true;

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
    }

    private void initButtonHandlers(){
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences prefs = getActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(EQ_ENABLED_PREF_KEY, checked);
                editor.apply();

                StreamingFragment streamingFragment = (StreamingFragment) getActivity().getFragmentManager().findFragmentByTag("streamingFragment");
                if (streamingFragment!=null)
                    streamingFragment.notifyEQEnabledSettingChanged();
            }
        });
    }
}
