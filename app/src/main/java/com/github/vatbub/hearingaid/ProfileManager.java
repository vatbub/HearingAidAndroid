package com.github.vatbub.hearingaid;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.vatbub.hearingaid.utils.ListUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the settings profiles (EQ on/off, eq setting, ...).<br>
 * <br>
 * Instances of this class are always tied to the calling {@link Context}. All instances use the same SharedPreferences,
 * therefore, all changes performed in one instance of this class will affect the data returned by all other instances.
 * Only data which is kept in the RAM is specific to each instance. Read the Javadoc of the getters to see which data is
 * shared across instances and which isn't.<br>
 * <br>
 * This class is threadsafe.
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
    private static volatile Profile currentlyActiveProfile;

    private ProfileManager(@NotNull Context callingContext) {
        setCallingContext(callingContext);
    }

    /**
     * Gets the instance associated with the specified context. Calling {@code getInstance(context)} multiple times
     * while supplying the same context will result in the same {@link ProfileManager} to be returned (singleton behaviour).<br>
     * <br>
     * <b>IMPORTANT:</b> The {@link ProfileManager} class only stores minimal information in RAM, most of its information is
     * instantly written to/read from the application's {@link SharedPreferences}. Nonetheless, please call {@link #resetInstance(Context)}
     * once the specified context is recycled to clean up memory.
     *
     * @param callingContext The context to get the {@link ProfileManager} for.
     * @return The {@link ProfileManager} for the specified context.
     */
    public static ProfileManager getInstance(@NotNull Context callingContext) {
        synchronized (instances) {
            if (!instances.containsKey(callingContext))
                instances.put(callingContext, new ProfileManager(callingContext));

            return instances.get(callingContext);
        }
    }

    /**
     * Resets the instance for the specified context. That means that all RAM memory used by that instance is released.
     * The next call to {@link #getInstance(Context)} will create a new instance for the specified context.
     * Call this method when the supplied context is about to be recycled.
     *
     * @param callingActivity The context to reset the instance for.
     * @return The id of the currently active {@link Profile}
     */
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

    /**
     * Returns the {@link ProfileManagerListener}s currently registered for this {@link ProfileManager}.
     * The content of this list is not shared across instances. Nevertheless, all listeners will listen for changes in any instance.
     *
     * @return the {@link ProfileManagerListener}s currently registered for this {@link ProfileManager}.
     */
    public List<ProfileManagerListener> getChangeListeners() {
        return changeListeners;
    }

    /**
     * Returns the currently active {@link Profile}. This data is <i>NOT</i> shared across instances.
     *
     * @return the currently active {@link Profile}.
     */
    @Nullable
    public Profile getCurrentlyActiveProfile() {
        return currentlyActiveProfile;
    }

    private void setCurrentlyActiveProfile(Profile currentlyActiveProfile) {
        ProfileManager.currentlyActiveProfile = currentlyActiveProfile;
    }

    /**
     * Returns a list of all available {@link Profile}s. This data <i>is</i> shared across instances.
     * This data <i>is</i> shared across instances.
     *
     * @return a list of all available {@link Profile}s.
     */
    public List<Profile> listProfiles() {
        List<Profile> res = new ArrayList<>();
        for (int id : getIDs()) {
            res.add(new Profile(id));
        }
        Collections.sort(res);
        return res;
    }

    /**
     * Sets the sorting order of profiles. {@link #listProfiles()} will return the profiles in that order.
     * This data <i>is</i> shared across instances.
     *
     * @param newOrder The new ordering of the profiles. NOTE: {@code newOrder} must contain all available profiles and
     *                 may not contain any extra profiles (i. e. its contents must be identical to {@link #listProfiles()} except for the order)
     */
    public void setOrder(List<Profile> newOrder) {
        List<Profile> currentOrder = listProfiles();
        if (!ListUtils.getInstance().isListEqualsWithoutOrder(currentOrder, newOrder))
            throw new IllegalArgumentException("newOrder must contain the same elements as listProfiles()");

        for (int i = 0; i < newOrder.size(); i++)
            newOrder.get(i).setSortPosition(i);

        for (ProfileManagerListener changeListener : getChangeListeners())
            changeListener.onSortOrderChanged(currentOrder, newOrder);
    }

    /**
     * Applies the profile with the specified id. This data is <i>NOT</i> shared across instances.
     *
     * @param id The id of the profile to apply.
     */
    public void applyProfile(int id) {
        applyProfile(new Profile(id));
    }

    /**
     * Applies the specified profile. This data is <i>NOT</i> shared across instances.
     *
     * @param profileToBeApplied The profile to be applied.
     */
    public void applyProfile(@Nullable Profile profileToBeApplied) {
        Profile previousProfile = getCurrentlyActiveProfile();

        if (previousProfile != null && previousProfile.equals(profileToBeApplied))
            return;

        setCurrentlyActiveProfile(profileToBeApplied);

        for (ProfileManagerListener changeListener : getChangeListenersForAllInstances()) {
            changeListener.onProfileApplied(previousProfile, profileToBeApplied);
        }
    }

    /**
     * Creates a new profile with the specified name. This data <i>is</i> shared across instances.
     *
     * @param profileName The name of the profile to create.
     * @return The new profile.
     */
    public Profile createProfile(String profileName) {
        Profile res = new Profile(profileName);
        for (ProfileManagerListener changeListener : getChangeListenersForAllInstances()) {
            changeListener.onProfileCreated(res);
        }
        return res;
    }

    /**
     * Deletes the specified profile. This data <i>is</i> shared across instances.
     *
     * @param profile The profile to be deleted.
     */
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

    /**
     * Returns the context tied to this instance.
     *
     * @return the context tied to this instance.
     */
    @NotNull
    public Context getCallingContext() {
        return callingContext;
    }

    private void setCallingContext(@NotNull Context callingContext) {
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

    /**
     * Returns the position of the specified profile in {@link #listProfiles()}
     *
     * @param profile The profile to get the position for.
     * @return The position of the specified profile in {@link #listProfiles()}
     */
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

    /**
     * Checks whether the specified id points to an existing profile. This data is <i>NOT</i> shared across instances.
     *
     * @param id The id to check.
     * @return {@code true} if the specified profile exists, {@code false} otherwise.
     */
    public boolean profileExists(int id) {
        return getIDs().contains(id);
    }

    /**
     * Listens to changes in the profile manager.<br>
     * <b>IMPORTANT NOTE:</b> {@code ProfileManagerListener}s are specific to each instance. However, all listeners will listen for changes in all instances.
     * This ensures that any listener will be notified about any change in the data.
     */
    public interface ProfileManagerListener {
        /**
         * Called <i>after</i> a new {@link Profile} has been applied.
         *
         * @param oldProfile The profile that was previously applied
         * @param newProfile The newly applied profile.
         */
        void onProfileApplied(@Nullable Profile oldProfile, @Nullable Profile newProfile);

        /**
         * Called <i>after</i> a new {@link Profile} has been created.
         *
         * @param newProfile The newly created profile.
         */
        void onProfileCreated(Profile newProfile);

        /**
         * Called just before a profile is deleted. Since the callback is called before the deletion of the profile, one can still access information from the profile in the callback.
         *
         * @param deletedProfile The profile about to be deleted
         */
        void onProfileDeleted(Profile deletedProfile);

        /**
         * Called <i>after</i> a new sort order has been applied using {@link #setOrder(List)}.
         *
         * @param previousOrder The previous order.
         * @param newOrder      The new order.
         */
        void onSortOrderChanged(List<Profile> previousOrder, List<Profile> newOrder);
    }

    private interface SetSettingRunnable {
        void setSetting(SharedPreferences.Editor editor);
    }

    /**
     * Represents a settings profile. Data retrieved from and supplied to this class is instantly read from/written to
     * permanent memory, no additional serialization required.<br>
     * <br>
     * <b>IMPORTANT NOTE:</b> The {@code Profile} class stores all of its data in the SharedPreferences.
     * Hence, multiple instances of the {@code Profile} class will all modify the same data as long as they
     * point to the same profile id.
     *
     * @see #getId()
     * @see #createProfile(String)
     * @see #deleteProfile(Profile)
     */
    public class Profile implements Comparable<Profile> {
        private int id;

        /**
         * Creates a new profile with the given name. For internal use only, for external use, see {@link #createProfile(String)}
         *
         * @param profileName The name of the profile to be created. Profile names may contain all characters and may duplicate themselves.
         */
        private Profile(String profileName) {
            setId(getNextProfileId());
            saveProfile(this);
            setSortPosition(getNextSortPos());
            setProfileName(profileName);
        }

        /**
         * Reads the profile with the specified id from memory.
         *
         * @param id The id of the profile to load.
         */
        public Profile(int id) {
            throwIfProfileDoesNotExist(id);

            setId(id);
        }

        private void throwIfProfileDoesNotExist() {
            throwIfProfileDoesNotExist(getId());
        }

        private void throwIfProfileDoesNotExist(int id) {
            if (!profileExists(id))
                throw new IndexOutOfBoundsException("The profile with the id " + id + " does not exist anymore or has never existed. Maybe it was deleted in the meanwhile?");
        }

        /**
         * Convenience method, same as {@link #profileExists(int)}
         *
         * @return See {@link #profileExists(int)}
         */
        public boolean exists() {
            return profileExists(getId());
        }

        /**
         * Returns the sorting position of this profile. Usually, you don't need to call this method directly as
         * {@link #listProfiles()} already sorts the profiles according to the sort order specified here.
         * @return the sorting position of this profile.
         * @see #listProfiles()
         * @see #setOrder(List)
         */
        public int getSortPosition() {
            throwIfProfileDoesNotExist();
            return getPrefs().getInt(generateProfilePrefKey(PROFILE_SORT_PREF_KEY), -1);
        }

        private void setSortPosition(int sortPosition) {
            setIntegerSetting(PROFILE_SORT_PREF_KEY, sortPosition);
        }

        /**
         * Returns an unmodifiable list of equalizer settings.
         * @return an unmodifiable list of equalizer settings.
         */
        public List<Float> getEQSettings() {
            throwIfProfileDoesNotExist();
            return Collections.unmodifiableList(getModifiableEQSettings());
        }

        /**
         * Sets the equalizer settings.
         * @param eqSettings The settings to save
         */
        public void setEQSettings(List<Float> eqSettings) {
            throwIfProfileDoesNotExist();
            deleteAllEqSettings();

            for (int i = 0; i < eqSettings.size(); i++) {
                setEQSetting(i, eqSettings.get(i));
            }
        }

        /**
         * Returns a modifiable list of equalizer settings. <b>IMPORTANT:</b> Call {@link EQSettingsList#apply()} to save your modifications.
         * @return a modifiable list of equalizer settings.
         */
        public EQSettingsList getModifiableEQSettings() {
            throwIfProfileDoesNotExist();
            EQSettingsList res = new EQSettingsList();
            SharedPreferences prefs = getPrefs();
            for (int i = 0; prefs.contains(generateEqPrefKey(i)); i++) {
                res.add(prefs.getFloat(generateEqPrefKey(i), -1));
            }
            return res;
        }

        /**
         * Sets one particular equalizer setting. <b>IMPORTANT: </b> Cannot create new eq settings, use {@link #setEQSettings(List)} to do so.
         *
         * @param index The index of the eq setting to modify.
         * @param value The new value.
         */
        public void setEQSetting(int index, float value) {
            throwIfProfileDoesNotExist();
            int eqCount = getEQSettings().size();
            if (eqCount > index)
                throw new ArrayIndexOutOfBoundsException("EQ index was out of range: index = " + index + ", current eq count = " + eqCount);

            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(generateEqPrefKey(index), value);
            editor.apply();
        }

        private String generateEqPrefKey(int index) {
            throwIfProfileDoesNotExist();
            return generateProfilePrefKey(EQ_SETTING_PREF_KEY) + "_" + index;
        }

        private String generateProfilePrefKey(String prefKey) {
            throwIfProfileDoesNotExist();
            return getId() + "." + prefKey;
        }

        private void deleteAllEqSettings() {
            throwIfProfileDoesNotExist();
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
            throwIfProfileDoesNotExist();
            SharedPreferences prefs = getPrefs();
            boolean res = prefs.contains(generateEqPrefKey(index));
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateEqPrefKey(index));
            editor.apply();

            return res;
        }

        /**
         * Returns whether the user enabled the equalizer.
         * @return whether the user enabled the equalizer.
         */
        public boolean isEqEnabled() {
            throwIfProfileDoesNotExist();
            return getPrefs().getBoolean(generateProfilePrefKey(EQ_ENABLED_PREF_KEY), EQ_ENABLED_DEFAULT_SETTING);
        }

        /**
         * Saves whether the user wants the equalizer to be enabled.
         * @param eqEnabled The new value.
         */
        public void setEqEnabled(boolean eqEnabled) {
            setBooleanSetting(EQ_ENABLED_PREF_KEY, eqEnabled);
        }

        public float getLowerHearingThreshold() {
            throwIfProfileDoesNotExist();
            return getPrefs().getFloat(generateProfilePrefKey(LOWER_HEARING_THRESHOLD_PREF_KEY), -1);
        }

        public void setLowerHearingThreshold(float lowerHearingThreshold) {
            setFloatSetting(LOWER_HEARING_THRESHOLD_PREF_KEY, lowerHearingThreshold);
        }

        public float getHigherHearingThreshold() {
            throwIfProfileDoesNotExist();
            return getPrefs().getFloat(generateProfilePrefKey(HIGHER_HEARING_THRESHOLD_PREF_KEY), -1);
        }

        public void setHigherHearingThreshold(float higherHearingThreshold) {
            setFloatSetting(HIGHER_HEARING_THRESHOLD_PREF_KEY, higherHearingThreshold);
        }

        /**
         * Returns the name of the profile.
         * @return the name of the profile.
         */
        public String getProfileName() {
            return getPrefs().getString(generateProfilePrefKey(PROFILE_NAME_PREF_KEY), null);
        }

        /**
         * Sets the name of this profile. Profiles may have duplicate names and may contain any characters which can be represented by a java string.
         * @param profileName The profile name to set
         */
        public void setProfileName(String profileName) {
            setStringSetting(PROFILE_NAME_PREF_KEY, profileName);
        }

        private void deleteSetting(String settingPrefKey) {
            throwIfProfileDoesNotExist();
            SharedPreferences prefs = getPrefs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(generateProfilePrefKey(settingPrefKey));
            editor.apply();
        }

        private void setFloatSetting(String prefKey, float value) {
            setSetting(editor -> editor.putFloat(generateProfilePrefKey(prefKey), value));
        }

        @SuppressWarnings("SameParameterValue")
        private void setStringSetting(String prefKey, String value) {
            setSetting(editor -> editor.putString(generateProfilePrefKey(prefKey), value));
        }

        @SuppressWarnings("SameParameterValue")
        private void setBooleanSetting(String prefKey, boolean value) {
            setSetting(editor -> editor.putBoolean(generateProfilePrefKey(prefKey), value));
        }

        @SuppressWarnings("SameParameterValue")
        private void setIntegerSetting(String prefKey, int value) {
            setSetting(editor -> editor.putInt(generateProfilePrefKey(prefKey), value));
        }

        private void setSetting(SetSettingRunnable setSettingRunnable) {
            throwIfProfileDoesNotExist();
            SharedPreferences.Editor editor = getPrefs().edit();
            setSettingRunnable.setSetting(editor);
            editor.apply();
        }

        /**
         * Deletes this profile from the shared preferences
         */
        private void delete() {
            deleteAllEqSettings();
            deleteSetting(LOWER_HEARING_THRESHOLD_PREF_KEY);
            deleteSetting(EQ_ENABLED_PREF_KEY);
            deleteSetting(HIGHER_HEARING_THRESHOLD_PREF_KEY);
            deleteSetting(PROFILE_SORT_PREF_KEY);
            deleteSetting(PROFILE_NAME_PREF_KEY);
        }

        /**
         * Returns {@code true} if this profile is the currently active profile in the related ProfileManager.
         * @return {@code true} if this profile is the currently active profile in the related ProfileManager.
         */
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
