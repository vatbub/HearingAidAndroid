package com.github.vatbub.hearingaid.com.github.vatbub.hearingaid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;

import com.github.vatbub.hearingaid.ProfileManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests the {@link com.github.vatbub.hearingaid.ProfileManager}
 */

public class ProfileManagerTest {
    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        resetProfiles();
    }

    @After
    public void cleanup() {
        resetProfiles();
    }

    private void resetProfiles() {
        ProfileManager.getInstance(context).getChangeListeners().clear();
        for (ProfileManager.Profile profile : ProfileManager.getInstance(context).listProfiles())
            ProfileManager.getInstance(context).deleteProfile(profile);
        ProfileManager.resetInstance(context);
    }

    @Test
    public void emptyProfileListTest() {
        Assert.assertTrue(ProfileManager.getInstance(context).listProfiles().isEmpty());
    }

    @Test
    public void applyProfileWithIdTest() {
        final boolean[] changeListenerCalled = {false};
        final ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("applyWithIdProfile");
        Assert.assertFalse(profile.isActive());
        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ProfileManagerListener() {
            @Override
            public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile, newProfile);
                changeListenerCalled[0] = true;
            }

            @Override
            public void onProfileCreated(ProfileManager.Profile newProfile) {

            }

            /**
             * Called just before a profile is deleted. Since the callback is called before the deletion of the profile, one can still access information from the profile in the callback.
             *
             * @param deletedProfile The profile about to be deleted
             */
            @Override
            public void onProfileDeleted(ProfileManager.Profile deletedProfile) {

            }

            @Override
            public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {

            }
        });
        ProfileManager.getInstance(context).applyProfile(profile.getId());
        Assert.assertEquals(profile, ProfileManager.getInstance(context).getCurrentlyActiveProfile());
        Assert.assertTrue(profile.isActive());
        Assert.assertTrue(changeListenerCalled[0]);
    }

    @Test
    public void applyProfileWithProfileObjectTest() {
        final boolean[] changeListenerCalled = {false};
        final ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("applyWithObjectProfile");
        Assert.assertFalse(profile.isActive());
        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ProfileManagerListener() {
            @Override
            public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile, newProfile);
                changeListenerCalled[0] = true;
            }

            @Override
            public void onProfileCreated(ProfileManager.Profile newProfile) {

            }

            /**
             * Called just before a profile is deleted. Since the callback is called before the deletion of the profile, one can still access information from the profile in the callback.
             *
             * @param deletedProfile The profile about to be deleted
             */
            @Override
            public void onProfileDeleted(ProfileManager.Profile deletedProfile) {

            }

            @Override
            public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {

            }
        });
        ProfileManager.getInstance(context).applyProfile(profile);
        Assert.assertEquals(profile, ProfileManager.getInstance(context).getCurrentlyActiveProfile());
        Assert.assertTrue(profile.isActive());
        Assert.assertTrue(changeListenerCalled[0]);
    }

    @Test
    public void resetInstanceTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("resetInstanceProfile");
        ProfileManager.getInstance(context).applyProfile(profile);
        int resetResult = ProfileManager.resetInstance(context);
        Assert.assertEquals(profile.getId(), resetResult);
    }

    @Test
    public void applyTwoProfilesTest() {
        final boolean[] changeListenerCalled = {false, false};
        final ProfileManager.Profile profile1 = ProfileManager.getInstance(context).createProfile("profile1");
        final ProfileManager.Profile profile2 = ProfileManager.getInstance(context).createProfile("profile2");

        Assert.assertFalse(profile1.isActive());
        Assert.assertFalse(profile2.isActive());

        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ProfileManagerListener() {
            @Override
            public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile1, newProfile);

                ProfileManager.getInstance(context).getChangeListeners().remove(this);
                changeListenerCalled[0] = true;
            }

            @Override
            public void onProfileCreated(ProfileManager.Profile newProfile) {

            }

            @Override
            public void onProfileDeleted(ProfileManager.Profile deletedProfile) {

            }

            @Override
            public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {

            }
        });

        ProfileManager.getInstance(context).applyProfile(profile1);

        Assert.assertTrue(profile1.isActive());
        Assert.assertFalse(profile2.isActive());

        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ProfileManagerListener() {
            @Override
            public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(profile1, oldProfile);
                Assert.assertEquals(profile2, newProfile);

                ProfileManager.getInstance(context).getChangeListeners().remove(this);
                changeListenerCalled[1] = true;
            }

            @Override
            public void onProfileCreated(ProfileManager.Profile newProfile) {

            }

            @Override
            public void onProfileDeleted(ProfileManager.Profile deletedProfile) {

            }

            @Override
            public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {

            }
        });

        ProfileManager.getInstance(context).applyProfile(profile2);

        Assert.assertFalse(profile1.isActive());
        Assert.assertTrue(profile2.isActive());
        Assert.assertTrue(changeListenerCalled[0]);
        Assert.assertTrue(changeListenerCalled[1]);
    }

    @Test
    public void createProfileTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("createProfileProfile");
        Assert.assertTrue(ProfileManager.getInstance(context).listProfiles().contains(profile));
    }

    @Test
    public void profileSortingTest() {
        int numberOfProfilesToCreate = 10;
        int incrCounter = 0;
        List<ProfileManager.Profile> newOrder = new LinkedList<>();
        for (int i = numberOfProfilesToCreate; i >= 1; i--) {
            ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("profileSortingTestProfile" + i);
            Assert.assertEquals(incrCounter, profile.getSortPosition());
            newOrder.add(0, profile);
            incrCounter++;
        }

        List<ProfileManager.Profile> expectedDefaultOrder = new ArrayList<>(newOrder);
        Collections.reverse(expectedDefaultOrder);
        List<ProfileManager.Profile> resultingList = ProfileManager.getInstance(context).listProfiles();

        Assert.assertEquals(expectedDefaultOrder, resultingList);

        // apply the new order
        ProfileManager.getInstance(context).setOrder(newOrder);

        resultingList = ProfileManager.getInstance(context).listProfiles();

        for (int j = 0; j < numberOfProfilesToCreate; j++)
            Assert.assertEquals(j, resultingList.get(j).getSortPosition());
    }

    @Test
    public void missingElementInNewOrderSortingTest() {
        int numberOfProfilesToCreate = 10;
        List<ProfileManager.Profile> newOrder = new LinkedList<>();
        for (int i = numberOfProfilesToCreate; i >= 1; i--) {
            ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("missingElementInNewOrderSortingTestProfile" + i);
            newOrder.add(0, profile);
        }
        newOrder.remove(0);

        try {
            ProfileManager.getInstance(context).setOrder(newOrder);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void excessiveElementInNewOrderSortingTest() {
        int numberOfProfilesToCreate = 10;
        List<ProfileManager.Profile> newOrder = new LinkedList<>();
        for (int i = numberOfProfilesToCreate; i >= 1; i--) {
            ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("excessiveElementInNewOrderSortingTestProfile" + i);
            newOrder.add(0, profile);
        }
        ProfileManager.getInstance(context).deleteProfile(newOrder.get(0));

        try {
            ProfileManager.getInstance(context).setOrder(newOrder);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void swapElementInProfilesListSortingTest() {
        int numberOfProfilesToCreate = 10;
        List<ProfileManager.Profile> newOrder = new LinkedList<>();
        for (int i = numberOfProfilesToCreate; i >= 1; i--) {
            ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("swapElementInProfilesListSortingTestProfile" + i);
            newOrder.add(0, profile);
        }
        ProfileManager.getInstance(context).deleteProfile(newOrder.get(numberOfProfilesToCreate - 1));
        ProfileManager.getInstance(context).createProfile("swapElementInProfilesListSortingTestSwappedProfile");

        try {
            ProfileManager.getInstance(context).setOrder(newOrder);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deleteProfileTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("deleteProfileProfile");
        ProfileManager.getInstance(context).deleteProfile(profile);
        Assert.assertFalse(ProfileManager.getInstance(context).listProfiles().contains(profile));
    }

    @Test
    public void deleteActiveProfileTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("deleteActiveProfileProfile");
        ProfileManager.getInstance(context).applyProfile(profile);
        ProfileManager.getInstance(context).deleteProfile(profile);
        Assert.assertFalse(profile.isActive());
    }

    @Test
    public void statePersistenceAcrossListCallsTest() {
        ProfileManager.Profile profile1 = ProfileManager.getInstance(context).createProfile("statePersistenceAcrossListCallsProfile1");
        ProfileManager.getInstance(context).applyProfile(profile1);
        for (ProfileManager.Profile profileUnderTest : ProfileManager.getInstance(context).listProfiles()) {
            if (profileUnderTest.equals(profile1))
                Assert.assertTrue(profileUnderTest.isActive());
            else
                Assert.assertFalse(profileUnderTest.isActive());
        }
    }

    @Test
    public void getPositionTest() {
        for (int i = 0; i < 5; i++) {
            ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("getPositionProfile" + i);
            Assert.assertEquals(i, ProfileManager.getInstance(context).getPosition(profile));
        }
    }

    @Test
    public void getPositionOfIllegalProfileTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("getPositionOfIllegalProfileTestProfile");
        ProfileManager.getInstance(context).deleteProfile(profile);
        Assert.assertEquals(-1, ProfileManager.getInstance(context).getPosition(profile));
    }

    @Test
    public void illegalIdReadTest() {
        try {
            ProfileManager.getInstance(context).new Profile(1);
            Assert.fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Expected IndexOutOfBoundsException occurred");
        }
    }

    @Test
    public void applySameProfileAgainTest() {
        final int[] changeListenerCalledCount = {0};
        final ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("applySameProfileAgainTestProfile");
        Assert.assertFalse(profile.isActive());
        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ProfileManagerListener() {
            @Override
            public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile, newProfile);
                changeListenerCalledCount[0]++;
            }

            @Override
            public void onProfileCreated(ProfileManager.Profile newProfile) {

            }

            @Override
            public void onProfileDeleted(ProfileManager.Profile deletedProfile) {

            }

            @Override
            public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {

            }
        });
        // apply twice
        ProfileManager.getInstance(context).applyProfile(profile);
        ProfileManager.getInstance(context).applyProfile(profile);
        Assert.assertEquals(profile, ProfileManager.getInstance(context).getCurrentlyActiveProfile());
        Assert.assertTrue(profile.isActive());
        Assert.assertEquals(1, changeListenerCalledCount[0]);
    }

    @Test
    public void eqEnabledSettingTest() {
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("eqEnabledSettingTestProfile");
        Assert.assertEquals(ProfileManager.EQ_ENABLED_DEFAULT_SETTING, profile.isEqEnabled());

        profile.setEqEnabled(true);
        Assert.assertEquals(true, profile.isEqEnabled());

        profile.setEqEnabled(false);
        Assert.assertEquals(false, profile.isEqEnabled());
    }

    @Test
    public void getProfileNameTest() {
        String name = "getProfileNameTestProfile";
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile(name);
        Assert.assertEquals(name, profile.getProfileName());
        Assert.assertEquals(name, profile.toString());
    }
}
