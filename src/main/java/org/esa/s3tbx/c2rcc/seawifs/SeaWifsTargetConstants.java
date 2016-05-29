package org.esa.s3tbx.c2rcc.seawifs;

import org.esa.s3tbx.c2rcc.util.BandDef;

/**
 * @author Marco Peters
 */
public class SeaWifsTargetConstants {
    static final float[] WAVELENGTHS = {412, 443, 490, 510, 555, 670, 765, 865};
    private static final String REFLEC_PREFIX = "reflec_";
    private static final String RHOT_PREFIX = "rhot_";

    public static BandDef[] getReflectances() {
        BandDef[] refls = new BandDef[WAVELENGTHS.length];
        for (int i = 0; i < WAVELENGTHS.length; i++) {
            float wavelength = WAVELENGTHS[i];
            String targetBandName = REFLEC_PREFIX + (int) wavelength;
            String sourceTemplateBandName = RHOT_PREFIX + (int) wavelength;
            refls[i] = new BandDef(targetBandName, sourceTemplateBandName, wavelength, 0, 1, 0);
        }
        return refls;
    }

}
