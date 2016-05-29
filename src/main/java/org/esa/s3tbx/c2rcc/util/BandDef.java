package org.esa.s3tbx.c2rcc.util;

/**
 * @author Marco Peters
 */
public class BandDef {
    String targetBandName;
    String sourceTemplateBandName;
    float wavelength;
    float bandwidth;
    int spectralIndex;
    float solarFlux;


    public BandDef(String targetBandName, String sourceTemplateBandName, float wavelength, float bandwidth, int spectralIndex, float solarFlux) {
        this.targetBandName = targetBandName;
        this.sourceTemplateBandName = sourceTemplateBandName;
        this.wavelength = wavelength;
        this.bandwidth = bandwidth;
        this.spectralIndex = spectralIndex;
        this.solarFlux = solarFlux;
    }

}
