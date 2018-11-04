package com.github.vatbub.hearingaid;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.vatbub.common.core.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Contains all code related to the Firebase remote config.
 */

public class RemoteConfig {
    private static final String defaultConfigFileName = "default_config.properties";
    private static final String configAppVersionKey = "configSavedWithAppVersion";
    private static Config config;

    public static void initConfig(Context context) {
        if (config != null) return;
        try {
            copyDefaultConfigToUserMemoryIfNotPresent(context);
            config = new Config(new URL("https://raw.githubusercontent.com/vatbub/HearingAidAndroid/master/app/src/main/assets/" + defaultConfigFileName), getCopyOfDefaultConfig(context).toURI().toURL(), true, "remote_config.properties", true);
        } catch (IOException e) {
            e.printStackTrace();
            BugsnagWrapper.notify(e);
        }
    }

    private static void copyDefaultConfigToUserMemoryIfNotPresent(Context context) throws IOException {
        InputStream configInputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".RemoteConfigInfo", Context.MODE_PRIVATE);
            File copyDestination = getCopyOfDefaultConfig(context);
            if (sharedPreferences.getInt(configAppVersionKey, BuildConfig.VERSION_CODE) <= BuildConfig.VERSION_CODE && copyDestination.exists())
                return;

            configInputStream = context.getAssets().open(defaultConfigFileName);
            fileOutputStream = new FileOutputStream(copyDestination, false);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = configInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
        } finally {
            if (configInputStream != null)
                configInputStream.close();
            if (fileOutputStream != null)
                fileOutputStream.close();
        }
    }

    private static File getCopyOfDefaultConfig(Context context) {
        return new File(context.getCacheDir(), defaultConfigFileName);
    }

    public static Config getConfig() {
        return config;
    }

    /**
     * Contains the parameter keys used in the config. Please keep in mind: If you change the key here (the value of the string), please also change it in the firebase console. Also, consider the fact that changing a key will require all users to update the app which will never be the case.
     */
    public static class Keys {
        public static final String LATENCY_MORE_INFO_URL = "fragmentStreamingLatencyMoreInfoURL";
        public static final String MOTD_URL = "motdURL";
        public static final String MOTD_CSS = "motdCSS";
        public static final String PLAY_STORE_URL = "playStoreURL";
        public static final String GIT_HUB_URL = "gitHubURL";
        public static final String MIN_EQ_FREQUENCY = "minEqFrequency";
        public static final String MAX_EQ_FREQUENCY = "maxEqFrequency";
        public static final String NUMBER_OF_EQ_BINS = "numberOfEqBins";
        public static final String EMAIL_FEEDBACK_TO_ADDRESS = "githubFeedbackToAddress";
        public static final String EMAIL_FEEDBACK_SUBJECT = "emailFeedbackSubject";
    }
}
