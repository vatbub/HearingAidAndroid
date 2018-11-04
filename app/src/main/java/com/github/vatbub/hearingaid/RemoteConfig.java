package com.github.vatbub.hearingaid;

import com.github.vatbub.common.core.Config;

import java.io.IOException;
import java.net.URL;

/**
 * Contains all code related to the Firebase remote config.
 */

public class RemoteConfig {
    private static Config config;

    public static void initConfig() {
        if (config != null) return;
        try {
            config = new Config(new URL("https://raw.githubusercontent.com/vatbub/HearingAidAndroid/master/app/src/main/assets/default_config.properties"), new URL("file:///android_asset/default_config.properties"), "remote_config.properties");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config getConfig() {
        initConfig();
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
