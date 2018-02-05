package com.github.vatbub.hearingaid.com.github.vatbub.hearingaid;

import android.content.Context;
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
    }

    @Test
    public void emptyProfileListTest() {
        Assert.assertTrue(ProfileManager.getInstance(context).listProfiles().isEmpty());
    }
}
