package com.github.vatbub.hearingaid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;

public class CustomApplication extends Application {
    private static final String BUG_SNAG_API_KEY_NOT_SPECIFIED = "not_specified";
    private static final String BUG_SNAG_ENABLED_KEY = "bugSnagEnabled";
    private static boolean bugSnagInitializable = false;
    private static boolean bugSnagInitialized = false;

    public static boolean isBugSnagInitializable() {
        return bugSnagInitializable;
    }

    public static void setBugSnagEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(BUG_SNAG_ENABLED_KEY, enabled);
        editor.apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".BugSnagConfig", MODE_PRIVATE);
    }

    public static boolean isBugSnagEnabled(Context context) {
        return getPrefs(context).getBoolean(BUG_SNAG_ENABLED_KEY, false);
    }

    public static boolean hasUserMadeAChoiceForBugsnag(Context context) {
        return getPrefs(context).contains(BUG_SNAG_ENABLED_KEY);
    }

    public static void initializeBugSnag(Context context) {
        if (isBugSnagInitialized()) return;
        if (!isBugSnagInitializable()) {
            Toast.makeText(context, R.string.bugsnag_api_key_not_specified_error_message, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isBugSnagEnabled(context)) return;

        Bugsnag.init(context);
        bugSnagInitialized = true;
    }

    public static boolean isBugSnagInitialized() {
        return bugSnagInitialized;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ApplicationInfo applicationInfo = getApplicationInfo();
        if (applicationInfo == null) return;
        Bundle metaData = applicationInfo.metaData;
        if (metaData == null) return;

        String bugSnagApiKey = metaData.getString("com.bugsnag.android.API_KEY");
        bugSnagInitializable = bugSnagApiKey != null && !bugSnagApiKey.equals(BUG_SNAG_API_KEY_NOT_SPECIFIED);

        initializeBugSnag(this);
    }
}
