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
        final ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("testProfile");
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
        Assert.assertTrue(changeListenerCalled[0]);
    }

    @Test
    public void applyProfileWithProfileObjectTest() {
        final boolean[] changeListenerCalled = {false};
        final ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("testProfile");
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
        Assert.assertTrue(changeListenerCalled[0]);
    }

    @Test
    public void resetInstanceTest(){
        ProfileManager.Profile profile = ProfileManager.getInstance(context).createProfile("testProfile");
        ProfileManager.getInstance(context).applyProfile(profile);
        int resetResult = ProfileManager.resetInstance(context);
        Assert.assertEquals(profile.getId(), resetResult);
    }
}
