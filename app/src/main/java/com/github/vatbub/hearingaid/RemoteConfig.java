package com.github.vatbub.hearingaid;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all code related to the Firebase remote config.
 */

public class RemoteConfig {
    public static void initConfig() {
        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        Map<String, Object> defaults = new HashMap<>();

        defaults.put(Keys.LATENCY_MORE_INFO_URL, "http://superpowered.com/android-audio-low-latency-primer");
        defaults.put(Keys.MOTD_URL, "https://fredplus10.me/hearing_aid_motd/feed/");
        defaults.put(Keys.MOTD_CSS, "body {font: 14px/24px \"Source Sans Pro\", sans-serif; background: rgba(0,0,0,0.0);}\n	a {text-decoration: none; -webkit-transition: all 0.3s ease-in-out; -moz-transition: all 0.3s ease-in-out; -ms-transition: all 0.3s ease-in-out; -o-transition: all 0.3s ease-in-out; transition: all 0.3s ease-in-out;}\n	a:hover, a:focus {color: #443f3f; text-decoration: none; outline: 0; -webkit-transition: all 0.3s ease-in-out; -moz-transition: all 0.3s ease-in-out; -ms-transition: all 0.3s ease-in-out; -o-transition: all 0.3s ease-in-out; transition: all 0.3s ease-in-out;}\n	img {max-width: 100%; height: auto;}\n	strong {font-weight: 600;}\n	h1 { font: 52px/1.1 \"Raleway\", sans-serif;}\n	h2 { font: 42px/1.1 \"Raleway\", sans-serif;}\n	h3 { font: 32px/1.1 \"Raleway\", sans-serif; text-align: center;}\n	h4 { font: 25px/1.1 \"Raleway\", sans-serif;}\n	h5 { font: 20px/1.1 \"Raleway\", sans-serif;}\n	h6 { font: 18px/1\n	.1 \"Raleway\", sans-serif;}\n	h1, h2, h3, h4, h5, h6 {color: #443f3f; font-weight: 600; margin: 10px 0 24px;}\n	table {width: 100%;}\n	th,td {border: 1px solid #333; padding: 1px; text-align: center;}\n	blockquote {border-left: 3px solid #d65050; background-color: #333; color: #fff; font-size: 16px; font-style: italic; line-height: 23px; margin-bottom: 30px; padding: 30px 35px; position: relative;}");
        defaults.put(Keys.PLAY_STORE_URL, "https://fredplus10.me/");
        defaults.put(Keys.MIN_EQ_FREQUENCY, 16);
        defaults.put(Keys.MAX_EQ_FREQUENCY, 21000);
        defaults.put(Keys.EMAIL_FEEDBACK_TO_ADDRESS, "feedback@fredplus10.me");
        defaults.put(Keys.EMAIL_FEEDBACK_SUBJECT, "[Feedback HearingAidAndroid]");

        remoteConfig.setDefaults(defaults);
        Task<Void> fetchTask = remoteConfig.fetch();
        fetchTask.addOnCompleteListener(task -> remoteConfig.activateFetched());
    }

    /**
     * Contains the parameter keys used in the config. Please keep in mind: If you change the key here (the value of the string), please also change it in the firebase console. Also, consider the fact that changing a key will require all users to update the app which will never be the case.
     */
    public static class Keys {
        public static final String LATENCY_MORE_INFO_URL = "fragmentStreamingLatencyMoreInfoURL";
        public static final String MOTD_URL = "motdURL";
        public static final String MOTD_CSS = "motdCSS";
        public static final String PLAY_STORE_URL = "playStoreURL";
        public static final String MIN_EQ_FREQUENCY = "minEqFrequency";
        public static final String MAX_EQ_FREQUENCY = "maxEqFrequency";
        public static final String EMAIL_FEEDBACK_TO_ADDRESS = "githubFeedbackToAddress";
        public static final String EMAIL_FEEDBACK_SUBJECT = "emailFeedbackSubject";
    }
}
