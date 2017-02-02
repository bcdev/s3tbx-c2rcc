package org.esa.s3tbx.c2rcc.util;

import org.esa.s3tbx.c2rcc.util.RgbProfiles;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class RgbProfilesTest {
    private static RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
    private static RGBImageProfile[] storedProfiles;

    @BeforeClass
    public static void setUpClass() throws Exception {
        storedProfiles = profileManager.getAllProfiles();
        removeProfiles(storedProfiles);
    }

    @Before
    public void setUp() throws Exception {
        removeProfiles(profileManager.getAllProfiles());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        RGBImageProfile[] allProfiles = profileManager.getAllProfiles();
        removeProfiles(allProfiles);
        for (RGBImageProfile profile : storedProfiles) {
            profileManager.addProfile(profile);
        }
    }

    @Test
    public void installMerisRgbProfiles() throws Exception {
        assertEquals(0, profileManager.getProfileCount());
        RgbProfiles.installMerisRgbProfiles();
        assertEquals(9, profileManager.getProfileCount());
    }

    private static void removeProfiles(RGBImageProfile[] storedProfiles) {
        for (RGBImageProfile profile : storedProfiles) {
            profileManager.removeProfile(profile);
        }
    }

}