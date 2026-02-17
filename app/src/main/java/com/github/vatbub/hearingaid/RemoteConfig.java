package com.github.vatbub.hearingaid;

import android.content.Context;
import android.content.SharedPreferences;



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
    private static Config config;

    public static void initConfig(Context context) {
        if (config != null) return;
        try {
            config = new Config(context);
        } catch (IOException e) {
            e.printStackTrace();
            BugsnagWrapper.notify(e);
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static class Config {
        private java.util.Properties properties;

        public Config(Context context) throws IOException {
            properties = new java.util.Properties();
            try (InputStream is = context.getAssets().open(defaultConfigFileName)) {
                properties.load(is);
            }
        }

        public String getValue(String key) {
            return properties.getProperty(key);
        }
    }

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
