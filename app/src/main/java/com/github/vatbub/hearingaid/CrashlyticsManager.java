package com.github.vatbub.hearingaid;

import android.content.Context;
import android.content.SharedPreferences;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * Simple singleton class to manage crashlytics easier
 */

public class CrashlyticsManager {
    private static final String PREF_NAME = "crashlyticsManager";
    private static final String CRASHLYTICS_ENABLED_PREF_KEY = "crashlyticsEnabled";
    private static final Map<Context, CrashlyticsManager> instances = new HashMap<>();
    private Context callingContext;
    private boolean defaultEnabledValue = true;

    private CrashlyticsManager(Context callingContext) {
        setCallingContext(callingContext);
    }

    public static CrashlyticsManager getInstance(Context callingContext) {
        synchronized (instances) {
            if (!instances.containsKey(callingContext))
                instances.put(callingContext, new CrashlyticsManager(callingContext));

            return instances.get(callingContext);
        }
    }

    public static void resetInstance(Context callingActivity) {
        synchronized (instances) {
            instances.remove(callingActivity);
        }
    }

    public void configureCrashlytics(){
        if (Fabric.isInitialized()) throw new IllegalStateException("Crashlytics is already configured");
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder().disabled(!isCrashlyticsEnabled()).build();
        Fabric.with(getCallingContext(), new Crashlytics.Builder().core(crashlyticsCore).build());
    }

    public void setCrashlyticsEnabled(boolean enabled){
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putBoolean(CRASHLYTICS_ENABLED_PREF_KEY, enabled);
        editor.apply();
    }

    public boolean isCrashlyticsEnabled(){
        System.out.println(getPrefs().getBoolean(CRASHLYTICS_ENABLED_PREF_KEY, getDefaultEnabledValue()));
        return getPrefs().getBoolean(CRASHLYTICS_ENABLED_PREF_KEY, getDefaultEnabledValue());
    }

    private SharedPreferences getPrefs(){
        return getCallingContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Context getCallingContext() {
        return callingContext;
    }

    public void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }

    public boolean getDefaultEnabledValue() {
        return defaultEnabledValue;
    }

    public void setDefaultEnabledValue(boolean defaultEnabledValue) {
        this.defaultEnabledValue = defaultEnabledValue;
    }
}
