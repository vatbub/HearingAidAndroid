package com.github.vatbub.hearingaid;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.vatbub.hearingaid.utils.ListUtils;

import java.util.ArrayList;
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
    public static final String PROFILE_NAME_PREF_KEY = "profileName";
    public static final String PROFILE_SORT_PREF_KEY = "sortPosition";
    public static final String EQ_ENABLED_PREF_KEY = "equalizerEnabled";
    public static final String EQ_SETTING_PREF_KEY = "eqSetting";
    public static final String IDS_PREF_KEY = "profileIDs";
    public static final String PROFILE_NAMES_DELIMITER = ";";
    public static final String LOWER_HEARING_THRESHOLD_PREF_KEY = "lowerHearingThreshold";
    public static final String HIGHER_HEARING_THRESHOLD_PREF_KEY = "higherHearingThreshold";
    public static final boolean EQ_ENABLED_DEFAULT_SETTING = true;
    private static final Map<Context, ProfileManager> instances = new HashMap<>();
    private final List<ProfileManagerListener> changeListeners = new ArrayList<>();
    private Context callingContext;
    private Profile currentlyActiveProfile;

    private ProfileManager(Context callingContext) {
        setCallingContext(callingContext);
    }

    public static ProfileManager getInstance(Context callingContext) {
        synchronized (instances) {
            if (!instances.containsKey(callingContext))
                instances.put(callingContext, new ProfileManager(callingContext));

            return instances.get(callingContext);
        }
    }

    public static int resetInstance(Context callingActivity) {
        synchronized (instances) {
            int res = -1;

            Profile currentProfile = getInstance(callingActivity).getCurrentlyActiveProfile();
            if (currentProfile != null)
                res = currentProfile.getId();

            instances.remove(callingActivity);
            return res;
        }
    }

    private static List<ProfileManagerListener> getChangeListenersForAllInstances() {
        List<ProfileManagerListener> res = new ArrayList<>();
        for (Map.Entry<Context, ProfileManager> entry : instances.entrySet())
            res.addAll(entry.getValue().getChangeListeners());

        return res;
    }

    public List<ProfileManagerListener> getChangeListeners() {
        return changeListeners;
    }

    @Nullable
    public Profile getCurrentlyActiveProfile() {
        return currentlyActiveProfile;
    }

    private void setCurrentlyActiveProfile(Profile currentlyActiveProfile) {
        this.currentlyActiveProfile = currentlyActiveProfile;
    }

    public List<Profile> listProfiles() {
        List<Profile> res = new ArrayList<>();
        for (int id : getIDs()) {
            res.add(new Profile(id));
        }
        Collections.sort(res);
        return res;
    }

    public void setOrder(List<Profile> newOrder) {
        List<Profile> currentOrder = listProfiles();
        if (!ListUtils.getInstance().isListEqualsWithoutOrder(currentOrder, newOrder))
            throw new IllegalArgumentException("newOrder must contain the same elements as listProfiles()");

        for (int i = 0; i < newOrder.size(); i++)
            newOrder.get(i).setSortPosition(i);

        for (ProfileManagerListener changeListener : getChangeListeners())
            changeListener.onSortOrderChanged(currentOrder, newOrder);
    }

    public void applyProfile(int id) {
        applyProfile(new Profile(id));
    }

    public void applyProfile(@Nullable Profile profileToBeApplied) {
        Profile previousProfile = getCurrentlyActiveProfile();

        if (previousProfile != null && previousProfile.equals(profileToBeApplied))
            return;

        setCurrentlyActiveProfile(profileToBeApplied);

        for (ProfileManagerListener changeListener : getChangeListenersForAllInstances()) {
            changeListener.onProfileApplied(previousProfile, profileToBeApplied);
        }
    }

    public Profile createProfile(String profileName) {
        Profile res = new Profile(profileName);
        for (ProfileManagerListener changeListener : getChangeListenersForAllInstances()) {
            changeListener.onProfileCreated(res);
        }
        return res;
    }

    public void deleteProfile(Profile profile) {
        if (profile.isActive())
            applyProfile(null);

        for (ProfileManagerListener changeListener : getChangeListenersForAllInstances()) {
            changeListener.onProfileDeleted(profile);
        }

        profile.delete();
        List<Integer> ids = getIDs();
        ids.remove((Integer) profile.getId());
        setIDs(ids);
    }

    public Context getCallingContext() {
        return callingContext;
    }

    private void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }

    private void saveProfile(Profile profile) {
        List<Integer> profileIDs = getIDs();
        if (!profileIDs.contains(profile.getId())) {
            profileIDs.add(profile.getId());
        }

        setIDs(profileIDs);
    }

    private List<Integer> getIDs() {
        String ids = getPrefs().getString(IDS_PREF_KEY, "");
        if (ids.isEmpty())
            return new ArrayList<>();
        List<Integer> res = new ArrayList<>();
        for (String id : ids.split(PROFILE_NAMES_DELIMITER))
            res.add(Integer.parseInt(id));

        return res;
    }

    private void setIDs(List<Integer> ids) {
        StringBuilder idsStringBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            idsStringBuilder.append(ids.get(i));
            if (i != ids.size() - 1)
                idsStringBuilder.append(PROFILE_NAMES_DELIMITER);
        }

        getPrefs().edit().putString(IDS_PREF_KEY, idsStringBuilder.toString()).apply();
    }

    private SharedPreferences getPrefs() {
        return getCallingContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public int getPosition(Profile profile) {
        List<Profile> profiles = listProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).equals(profile))
                return i;
        }
        return -1;
    }

    private int getNextProfileId() {
        List<Integer> ids = getIDs();
        if (ids.isEmpty())
            return 1;

        return Collections.max(ids) + 1;
    }

    /**
     * Returns the next sort position for new profiles. Expects the profile list obtained using {@link #listProfiles()} to be sorted by the sort position.
     *
     * @return The next sort pos.
     */
    private int getNextSortPos() {
        List<Profile> profiles = listProfiles();

        if (profiles.isEmpty())
            return 0;

        return profiles.get(profiles.size() - 1).getSortPosition() + 1;
    }

    public boolean profileExists(int id) {
        return getIDs().contains(id);
    }

    public interface ProfileManagerListener {
        void onProfileApplied(@Nullable Profile oldProfile, @Nullable Profile newProfile);

        void onProfileCreated(Profile newProfile);

        /**
         * Called just before a profile is deleted. Since the callback is called before the deletion of the profile, one can still access information from the profile in the callback.
         *
         * @param deletedProfile The profile about to be deleted
         */
        void onProfileDeleted(Profile deletedProfile);

        void onSortOrderChanged(List<Profile> previousOrder, List<Profile> newOrder);
    }

    public class Profile implements Comparable<Profile> {
        private int id;

        /**
         * Creates a new profile with the given name. For internal use only, for external use, see {@link #createProfile(String)}
         *
         * @param profileName The name of the profile to be created. Profile names may contain all characters and may duplicate themselves.
         */
        private Profile(String profileName) {
            setId(getNextProfileId());
            setSortPosition(getNextSortPos());
            saveProfile(this);
            setProfileName(profileName);
        }

        /**
         * Reads the profile with the specified id from memory.
         *
         * @param id The id of the profile to load.
         */
        public Profile(int id) {
            if (!profileExists(id))
                throw new IndexOutOfBoundsException();

            setId(id);
        }

        public int getSortPosition() {
            return getPrefs().getInt(generateProfilePrefKey(PROFILE_SORT_PREF_KEY), -1);
        }

        private void setSortPosition(int sortPosition) {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(generateProfilePrefKey(PROFILE_SORT_PREF_KEY), sortPosition);
            editor.apply();
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
            return getId() + "." + prefKey;
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

        public void deleteSortPosition() {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateProfilePrefKey(PROFILE_SORT_PREF_KEY));
            editor.apply();
        }

        public String getProfileName() {
            return getPrefs().getString(generateProfilePrefKey(PROFILE_NAME_PREF_KEY), null);
        }

        public void setProfileName(String profileName) {
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(generateProfilePrefKey(PROFILE_NAME_PREF_KEY), profileName);
            editor.apply();
        }

        /**
         * Deletes this profile from the shared preferences
         */
        private void delete() {
            deleteAllEqSettings();
            deleteEqEnabled();
            deleteLowerHearingThreshold();
            deleteHigherHearingThreshold();
            deleteSortPosition();
        }

        public boolean isActive() {
            return getCurrentlyActiveProfile() != null && getCurrentlyActiveProfile().equals(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Profile && ((Profile) obj).getId() == this.getId();
        }

        @Override
        public String toString() {
            return getProfileName();
        }

        public int getId() {
            return id;
        }

        private void setId(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(@NonNull Profile that) {
            if (this.getSortPosition() < that.getSortPosition())
                return -1;
            else if (this.getSortPosition() > that.getSortPosition())
                return 1;

            return 0;
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
