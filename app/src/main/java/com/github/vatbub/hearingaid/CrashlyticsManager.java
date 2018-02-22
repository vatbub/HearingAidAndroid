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

    /**
     * Configures crashlytics for the current app session. Please note: This is a no-op if called more than once.
     * This is because Crashlytics can only be configured once per app session.
     */
    public void configureCrashlytics(){
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder().disabled(!isCrashlyticsEnabled()).build();
        Fabric.with(getCallingContext(), new Crashlytics.Builder().core(crashlyticsCore).build());
    }

    /**
     * Sets the setting whether to enable crashlytics or not. Please note: To apply the setting, the app must be restarted and {@link #configureCrashlytics()} must be called after the restart.
     * @param enabled The setting to set.
     */
    public void setCrashlyticsEnabled(boolean enabled){
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putBoolean(CRASHLYTICS_ENABLED_PREF_KEY, enabled);
        editor.apply();
    }

    /**
     * Returns the current crashlytics setting. Please note: This method will return the value that was set at the last call of {@link #setCrashlyticsEnabled(boolean)}.
     * If {@link #configureCrashlytics()} has been called prior to that, Crashlytics might still be active even though this method returns false. See {@link #setCrashlyticsEnabled(boolean)} for more info.
     * @see #setCrashlyticsEnabled(boolean)
     * @return the current crashlytics setting.
     */
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

    private void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }

    public boolean getDefaultEnabledValue() {
        return defaultEnabledValue;
    }

    public void setDefaultEnabledValue(boolean defaultEnabledValue) {
        this.defaultEnabledValue = defaultEnabledValue;
    }
}
