package com.github.vatbub.hearingaid.com.github.vatbub.hearingaid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;

import com.github.vatbub.hearingaid.ProfileManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link com.github.vatbub.hearingaid.ProfileManager}
 */

public class ProfileManagerTest {
    Context context;

    @Before
    public void prepareContext() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void resetProfiles() {
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
        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ActiveProfileChangeListener() {
            @Override
            public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile, newProfile);
                changeListenerCalled[0] = true;
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
        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ActiveProfileChangeListener() {
            @Override
            public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile, newProfile);
                changeListenerCalled[0] = true;
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

        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ActiveProfileChangeListener() {
            @Override
            public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(null, oldProfile);
                Assert.assertEquals(profile1, newProfile);

                ProfileManager.getInstance(context).getChangeListeners().remove(this);
                changeListenerCalled[0] = true;
            }
        });

        ProfileManager.getInstance(context).applyProfile(profile1);

        Assert.assertTrue(profile1.isActive());
        Assert.assertFalse(profile2.isActive());

        ProfileManager.getInstance(context).getChangeListeners().add(new ProfileManager.ActiveProfileChangeListener() {
            @Override
            public void onChanged(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
                Assert.assertEquals(profile1, oldProfile);
                Assert.assertEquals(profile2, newProfile);

                ProfileManager.getInstance(context).getChangeListeners().remove(this);
                changeListenerCalled[1] = true;
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
        ProfileManager.Profile profile2 = ProfileManager.getInstance(context).createProfile("statePersistenceAcrossListCallsProfile2");
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
    public void illegalIdReadTest(){
        try{
            ProfileManager.getInstance(context).new Profile(1);
            Assert.fail("IndexOutOfBoundsException expected");
        }catch(IndexOutOfBoundsException e){
            System.out.println("Expected IndexOutOfBoundsException occurred");
        }
    }
}
