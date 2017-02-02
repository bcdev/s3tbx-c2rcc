package org.esa.s3tbx.c2rcc.util;

import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Marco Peters
 */
public class RgbProfiles {
    private static final String[] RGB_PROFILE_NAMES = {
            "c2rcc_rhow", "c2rcc_rrs", "c2rcc_rhown", "c2rcc_rpath", "c2rcc_rtoa",
            "c2rcc_rtosa_gc", "c2rcc_rtosagc_aann", "c2rcc_tdown", "c2rcc_tup"
    };

    public static void installMerisRgbProfiles() {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        RGBImageProfile[] allProfiles = profileManager.getAllProfiles();
        for (String profileName : RGB_PROFILE_NAMES) {
            Stream<RGBImageProfile> profileStream = Arrays.stream(allProfiles);
            boolean profileExists = profileStream.anyMatch(rgbImageProfile -> rgbImageProfile.getName().equals(profileName));
            if (!profileExists) {
                profileManager.addProfile(new RGBImageProfile(profileName, new String[]{
                        String.format("log(0.05 + 0.35 * %1$s_2 + 0.60 * %1$s_5 + %1$s_6 + 0.13 * %1$s_7)", profileName),
                        String.format("log(0.05 + 0.21 * %1$s_3 + 0.50 * %1$s_4 + %1$s_5 + 0.38 * %1$s_6)", profileName),
                        String.format("log(0.05 + 0.21 * %1$s_1 + 1.75 * %1$s_2 + 0.47 * %1$s_3 + 0.16 * %1$s_4)", profileName)}));
            }
        }
    }
}
