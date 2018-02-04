package com.github.vatbub.hearingaid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.vatbub.hearingaid.fragments.StreamingFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the settings profiles (EQ on/off, eq setting, ...)
 */

public class ProfileManager {
    public static final String SETTINGS_SHARED_PREFERENCES_NAME = "hearingAidSettings";
    public static final String EQ_ENABLED_PREF_KEY = "equalizerEnabled";
    public static final String EQ_SETTING_PREF_KEY = "eqSetting";
    public static final String PROFILE_NAMES_PREF_KEY = "profileNames";
    public static final String PROFILE_NAMES_DELIMITER = ";";
    public static final String LOWER_HEARING_THRESHOLD_PREF_KEY = "lowerHearingThreshold";
    public static final String HIGHER_HEARING_THRESHOLD_PREF_KEY = "higherHearingThreshold";
    public static final boolean EQ_ENABLED_DEFAULT_SETTING = true;
    private static Map<Activity, ProfileManager> instances;
    private List<ActiveProfileChangeListener> changeListeners = new ArrayList<>();
    private Activity callingActivity;
    private Profile currentlyActiveProfile;

    private ProfileManager(Activity callingActivity) {
        setCallingActivity(callingActivity);
    }

    /**
     * Returns a list of characters/strings that must not be used in profile names.
     *
     * @return A list of characters/strings that must not be used in profile names.
     */
    public static List<String> illegalCharacters() {
        List<String> res = new ArrayList<>();
        res.add(PROFILE_NAMES_DELIMITER);
        return res;
    }

    public static ProfileManager getInstance(Activity callingActivity) {
        if (instances == null)
            instances = new HashMap<>();
        if (!instances.containsKey(callingActivity))
            instances.put(callingActivity, new ProfileManager(callingActivity));

        return instances.get(callingActivity);
    }

    @Nullable
    public static String resetInstance(Activity callingActivity) {
        String res = null;

        if (getInstance(callingActivity).getCurrentlyActiveProfile() != null)
            res = getInstance(callingActivity).getCurrentlyActiveProfile().getProfileName();

        instances.remove(callingActivity);
        return res;
    }

    public List<ActiveProfileChangeListener> getChangeListeners() {
        return changeListeners;
    }

    public Profile getCurrentlyActiveProfile() {
        return currentlyActiveProfile;
    }

    private void setCurrentlyActiveProfile(Profile currentlyActiveProfile) {
        this.currentlyActiveProfile = currentlyActiveProfile;
    }

    public List<Profile> listProfiles() {
        List<Profile> res = new ArrayList<>();
        for (String profileName : getProfileNames()) {
            res.add(new Profile(profileName, true));
        }
        return res;
    }

    public void applyProfile(int index) {
        applyProfile(listProfiles().get(index));
    }

    public void applyProfile(String profileName) {
        applyProfile(new Profile(profileName, true));
    }

    public void applyProfile(@Nullable Profile profileToBeApplied) {
        Profile previousProfile = getCurrentlyActiveProfile();
        if (previousProfile != null)
            previousProfile.setActive(false);

        setCurrentlyActiveProfile(profileToBeApplied);
        if (profileToBeApplied != null)
            profileToBeApplied.setActive(true);

        for (ActiveProfileChangeListener changeListener : getChangeListeners()) {
            changeListener.onChanged(previousProfile, profileToBeApplied);
        }
    }

    public Profile createProfile(String profileName) {
        // TODO: Check for uniqueness
        // TODO: CHeck for illegal characters
        return new Profile(profileName);
    }

    public void deleteProfile(Profile profile) {
        if (profile.isActive())
            applyProfile((Profile) null);
        profile.delete();
        List<String> profileNames = getProfileNames();
        profileNames.remove(profile.getProfileName());
        setProfileNames(profileNames);
    }

    public Activity getCallingActivity() {
        return callingActivity;
    }

    private void setCallingActivity(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

    private void saveProfile(Profile profile, String oldProfileName) {
        List<String> profileNames = getProfileNames();
        if (!profileNames.contains(profile.getProfileName())) {
            if (profileNames.contains(oldProfileName)) {
                // replace the old one
                int index = profileNames.indexOf(oldProfileName);
                profileNames.set(index, profile.getProfileName());
            } else {
                profileNames.add(profile.getProfileName());
            }
        }

        setProfileNames(profileNames);
    }

    private List<String> getProfileNames() {
        String profileNames = getPrefs().getString(PROFILE_NAMES_PREF_KEY, "");
        if (profileNames.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(profileNames.split(PROFILE_NAMES_DELIMITER)));
    }

    private void setProfileNames(List<String> profileNames) {
        StringBuilder profileNamesStringBuilder = new StringBuilder();
        for (int i = 0; i < profileNames.size(); i++) {
            profileNamesStringBuilder.append(profileNames.get(i));
            if (i != profileNames.size() - 1)
                profileNamesStringBuilder.append(PROFILE_NAMES_DELIMITER);
        }

        getPrefs().edit().putString(PROFILE_NAMES_PREF_KEY, profileNamesStringBuilder.toString()).apply();
    }

    private SharedPreferences getPrefs() {
        return getCallingActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public int getPosition(Profile profile) {
        List<Profile> profiles = listProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).equals(profile))
                return i;
        }
        return -1;
    }

    public interface ActiveProfileChangeListener {
        void onChanged(@Nullable Profile oldProfile, @Nullable Profile newProfile);
    }

    public class Profile {
        private String profileName;
        private boolean active;

        private Profile(String profileName) {
            this(profileName, false);
        }

        /**
         * For internal use only.
         * Same as {@link #Profile(String)} except if {@code skipSave} is set to {@code true}
         *
         * @param profileName The name of the profile
         * @param skipSave    If set to {@code true}, the profile will not notify the profile manager about its creation.
         *                    That is to prevent unnecessary save operations when the profile manager instantiates a new Profile image
         *                    after reading it from the preferences.
         */
        private Profile(String profileName, boolean skipSave) {
            if (skipSave)
                this.profileName = profileName;
            else
                setProfileName(profileName);
        }

        public List<Float> getEQSettings() {
            return Collections.unmodifiableList(getModifiableEQSettings());
        }

        public void setEQSettings(List<Float> eqSettings) {
            deleteAllEqSettings();

            for (int i = 0; i < eqSettings.size(); i++) {
                setEQSettings(i, eqSettings.get(i));
            }
        }

        public EQSettingsList getModifiableEQSettings() {
            EQSettingsList res = new EQSettingsList();
            SharedPreferences prefs = getPrefs();
            for (int i = 0; prefs.contains(generateEqPrefKey(i)); i++) {
                res.add(prefs.getFloat(generateEqPrefKey(i), -1));
            }
            return res;
        }

        public void setEQSettings(int index, float value) {
            int eqCount = getEQSettings().size();
            if (eqCount > index)
                throw new ArrayIndexOutOfBoundsException("EQ index was out of range: index = " + index + ", current eq count = " + eqCount);

            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(generateEqPrefKey(index), value);
            editor.apply();
        }

        private String generateEqPrefKey(int index) {
            return generateProfilePrefKey(EQ_SETTING_PREF_KEY) + "_" + index;
        }

        private String generateProfilePrefKey(String prefKey) {
            return getProfileName() + "." + prefKey;
        }

        public void deleteAllEqSettings() {
            boolean cont = true;
            for (int i = 0; cont; i++) {
                cont = deleteEqSetting(i);
            }
        }

        /**
         * Deletes the specified eq setting. Be careful when deleting eq settings as deleting arbitrary settings may corrupt the settings. Consider using {@link #setEQSettings(List)} instead
         *
         * @param index The index of the eq setting to delete.
         * @return {@code true} if the setting was actually deleted, {@code false} if not (e. g. because it was never defined).
         */
        private boolean deleteEqSetting(int index) {
            SharedPreferences prefs = getPrefs();
            boolean res = prefs.contains(generateEqPrefKey(index));
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateEqPrefKey(index));
            editor.apply();

            return res;
        }

        public boolean isEqEnabled() {
            SharedPreferences prefs = getPrefs();
            return prefs.getBoolean(generateProfilePrefKey(EQ_ENABLED_PREF_KEY), EQ_ENABLED_DEFAULT_SETTING);
        }

        public void setEqEnabled(boolean eqEnabled) {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(generateProfilePrefKey(EQ_ENABLED_PREF_KEY), eqEnabled);
            editor.apply();
        }

        public float getLowerHearingThreshold() {
            return getPrefs().getFloat(generateProfilePrefKey(LOWER_HEARING_THRESHOLD_PREF_KEY), -1);
        }

        public void setLowerHearingThreshold(float lowerHearingThreshold) {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(generateProfilePrefKey(LOWER_HEARING_THRESHOLD_PREF_KEY), lowerHearingThreshold);
            editor.apply();
        }

        public void deleteLowerHearingThreshold() {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateProfilePrefKey(LOWER_HEARING_THRESHOLD_PREF_KEY));
            editor.apply();
        }

        public float getHigherHearingThreshold() {
            return getPrefs().getFloat(generateProfilePrefKey(HIGHER_HEARING_THRESHOLD_PREF_KEY), -1);
        }

        public void setHigherHearingThreshold(float higherHearingThreshold) {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(generateProfilePrefKey(HIGHER_HEARING_THRESHOLD_PREF_KEY), higherHearingThreshold);
            editor.apply();
        }

        public void deleteHigherHearingThreshold() {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateProfilePrefKey(HIGHER_HEARING_THRESHOLD_PREF_KEY));
            editor.apply();
        }

        public void deleteEqEnabled() {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateProfilePrefKey(EQ_ENABLED_PREF_KEY));
            editor.apply();
        }

        public String getProfileName() {
            return profileName;
        }

        public void setProfileName(String profileName) {
            String oldProfileName = this.profileName;

            // transfer all data from the old name to the new one
            EQSettingsList eqSettings = getModifiableEQSettings();
            boolean eqEnabled = isEqEnabled();
            float lowerHearingThreshold = getLowerHearingThreshold();
            float higherHearingThreshold = getHigherHearingThreshold();

            delete();

            this.profileName = profileName;

            setEQSettings(eqSettings);
            setEqEnabled(eqEnabled);
            setLowerHearingThreshold(lowerHearingThreshold);
            setHigherHearingThreshold(higherHearingThreshold);

            saveProfile(this, oldProfileName);
        }

        /**
         * Deletes this profile from the shared preferences
         */
        private void delete() {
            deleteAllEqSettings();
            deleteEqEnabled();
            deleteLowerHearingThreshold();
            deleteHigherHearingThreshold();
        }

        public boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;

            if (active) {
                StreamingFragment streamingFragment = (StreamingFragment) getCallingActivity().getFragmentManager().findFragmentByTag("streamingFragment");
                if (streamingFragment != null)
                    streamingFragment.notifyEQEnabledSettingChanged();
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Profile && ((Profile) obj).getProfileName().equalsIgnoreCase(this.getProfileName());
        }

        @Override
        public String toString() {
            return getProfileName();
        }

        public class EQSettingsList extends ArrayList<Float> {
            public EQSettingsList(int initialCapacity) {
                super(initialCapacity);
            }

            public EQSettingsList() {
            }

            public EQSettingsList(@NonNull Collection<? extends Float> c) {
                super(c);
            }

            public void apply() {
                setEQSettings(this);
            }
        }
    }
}
