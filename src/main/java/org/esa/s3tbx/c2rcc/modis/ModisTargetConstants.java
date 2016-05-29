package org.esa.s3tbx.c2rcc.modis;

import org.esa.s3tbx.c2rcc.util.BandDef;

/**
 * @author Marco Peters
 */
public class ModisTargetConstants {
    static final float[] WAVELENGTHS = {412, 443, 488, 531, 547, 667, 678, 748, 869};
    static final float[] BAND_WIDTH = new float[WAVELENGTHS.length];
    static final float[] SOLAR_FLUX = new float[WAVELENGTHS.length];
    private static final String REFLEC_PREFIX = "reflec_";
    private static final String RHOT_PREFIX = "rhot_";

    public static BandDef[] getReflectances() {
        BandDef[] refls = new BandDef[WAVELENGTHS.length];
        for (int i = 0; i < WAVELENGTHS.length; i++) {
            float wavelength = WAVELENGTHS[i];
            String targetBandName = REFLEC_PREFIX + (int) wavelength;
            String sourceTemplateBandName = RHOT_PREFIX + (int) wavelength;
            float bandwidth = BAND_WIDTH[i];
            float solarFlux = SOLAR_FLUX[i];
            refls[i] = new BandDef(targetBandName, sourceTemplateBandName, wavelength, bandwidth, 1, solarFlux);
        }
        return refls;
    }

}
