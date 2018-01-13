package com.github.vatbub.hearingaid;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
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

        remoteConfig.setDefaults(defaults);
        Task<Void> fetchTask = remoteConfig.fetch();
        fetchTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                remoteConfig.activateFetched();
            }
        });
    }

    /**
     * Contains the parameter keys used in the config. Please keep in mind: If you change the key here (the value of the string), please also change it in the firebase console. Also, consider the fact that changing a key will require all users to update the app which will never be the case.
     */
    public static class Keys {
        public static final String LATENCY_MORE_INFO_URL = "fragmentStreamingLatencyMoreInfoURL";
    }
}
