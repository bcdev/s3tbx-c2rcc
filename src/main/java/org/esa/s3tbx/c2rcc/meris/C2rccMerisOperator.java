package org.esa.s3tbx.c2rcc.meris;

import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.DEFAULT_SOLAR_FLUX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband12_ix;

import org.esa.s3tbx.c2rcc.util.SolarFluxLazyLookup;
import org.esa.s3tbx.c2rcc.util.TargetProductPreparer;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.pointop.PixelOperator;
import org.esa.snap.framework.gpf.pointop.ProductConfigurer;
import org.esa.snap.framework.gpf.pointop.Sample;
import org.esa.snap.framework.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.framework.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.framework.gpf.pointop.WritableSample;
import org.esa.snap.util.converters.BooleanExpressionConverter;

import java.io.IOException;
import java.util.Calendar;

// todo (nf) - Add Thullier solar fluxes as default values to C2R-CC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MERIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MERIS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "meris.c2rcc", version = "0.5",
            authors = "Roland Doerffer, Norman Fomferra (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on MERIS L1b data products.")
public class C2rccMerisOperator extends PixelOperator {

    // MERIS sources
    public static final int BAND_COUNT = 15;
    public static final int DEM_ALT_IX = BAND_COUNT;
    public static final int SUN_ZEN_IX = BAND_COUNT + 1;
    public static final int SUN_AZI_IX = BAND_COUNT + 2;
    public static final int VIEW_ZEN_IX = BAND_COUNT + 3;
    public static final int VIEW_AZI_IX = BAND_COUNT + 4;
    public static final int ATM_PRESS_IX = BAND_COUNT + 5;
    public static final int OZONE_IX = BAND_COUNT + 6;

    // MERIS targets
    public static final int REFLEC_N = merband12_ix.length;

    public static final int REFLEC_1_IX = 0;
    public static final int IOP_APIG_IX = REFLEC_N;
    public static final int IOP_ADET_IX = REFLEC_N + 1;
    public static final int IOP_AGELB_IX = REFLEC_N + 2;
    public static final int IOP_BPART_IX = REFLEC_N + 3;
    public static final int IOP_BWIT_IX = REFLEC_N + 4;

    public static final int RTOSA_RATIO_MIN_IX = REFLEC_N + 5;
    public static final int RTOSA_RATIO_MAX_IX = REFLEC_N + 6;
    public static final int L2_QFLAGS_IX = REFLEC_N + 7;

    public static final int RTOSA_IN_1_IX = REFLEC_N + 8;
    public static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + REFLEC_N;

    @SourceProduct(label = "MERIS L1b product",
                description = "MERIS L1b source product.")
    private Product sourceProduct;

    @Parameter(label = "Valid-pixel expression",
                defaultValue = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "false")
    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    private C2rccMerisAlgorithm algorithm;
    private SolarFluxLazyLookup solarFluxLazyLookup;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] radiances = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
        }

        PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        if (useDefaultSolarFlux) {
            double mjd = sourceProduct.getTimeCoding().getMJD(pixelPos);
            ProductData.UTC utc = new ProductData.UTC(mjd);
            Calendar calendar = utc.getAsCalendar();
            final int doy = calendar.get(Calendar.DAY_OF_YEAR);
            final int year = calendar.get(Calendar.YEAR);
            double[] correctedSolFlux = solarFluxLazyLookup.getCorrectedFluxFor(doy, year);
            algorithm.setSolflux(correctedSolFlux);
        }

        // use real geocoding if needed
        GeoPos geoPos = new GeoPos(0, 0);
//        GeoPos geoPos = sourceProduct.getGeoCoding().getGeoPos(pixelPos, null);
        C2rccMerisAlgorithm.Result result = algorithm.processPixel(x, y, geoPos.getLat(), geoPos.getLon(),
                                                                   radiances,
                                                                   sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                   sourceSamples[SUN_AZI_IX].getDouble(),
                                                                   sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                   sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                   sourceSamples[DEM_ALT_IX].getDouble(),
                                                                   sourceSamples[ATM_PRESS_IX].getDouble(),
                                                                   sourceSamples[OZONE_IX].getDouble());

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }

        targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
        targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);
        targetSamples[L2_QFLAGS_IX].set(result.flags);

        if (outputRtosa) {
            for (int i = 0; i < result.rtosa_in.length; i++) {
                targetSamples[RTOSA_IN_1_IX + i].set(result.rtosa_in[i]);
            }
            for (int i = 0; i < result.rtosa_out.length; i++) {
                targetSamples[RTOSA_OUT_1_IX + i].set(result.rtosa_out[i]);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < BAND_COUNT; i++) {
            sc.defineSample(i, "radiance_" + (i + 1));
        }
        sc.defineSample(DEM_ALT_IX, "dem_alt");
        sc.defineSample(SUN_ZEN_IX, "sun_zenith");
        sc.defineSample(SUN_AZI_IX, "sun_azimuth");
        sc.defineSample(VIEW_ZEN_IX, "view_zenith");
        sc.defineSample(VIEW_AZI_IX, "view_azimuth");
        sc.defineSample(ATM_PRESS_IX, "atm_press");
        sc.defineSample(OZONE_IX, "ozone");
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {

        for (int i = 0; i < merband12_ix.length; i++) {
            int bi = merband12_ix[i];
            sc.defineSample(REFLEC_1_IX + i, "reflec_" + bi);
        }

        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");
        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(L2_QFLAGS_IX, "l2_qflags");

        if (outputRtosa) {
            for (int i = 0; i < merband12_ix.length; i++) {
                int bi = merband12_ix[i];
                sc.defineSample(RTOSA_IN_1_IX + i, "rtosa_in_" + bi);
            }
            for (int i = 0; i < merband12_ix.length; i++) {
                int bi = merband12_ix[i];
                sc.defineSample(RTOSA_OUT_1_IX + i, "rtosa_out_" + bi);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        Product targetProduct = productConfigurer.getTargetProduct();
        TargetProductPreparer.prepareTargetProduct(targetProduct, sourceProduct, "radiance_", merband12_ix, outputRtosa);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        for (int i = 0; i < BAND_COUNT; i++) {
            assertSourceBand("radiance_" + (i + 1));
        }
        assertSourceBand("l1_flags");

        // todo must be reactivated later
//        if (sourceProduct.getGeoCoding() == null) {
//            throw new OperatorException("The source product must be geo-coded.");
//        }

        try {
            algorithm = new C2rccMerisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
        if (!useDefaultSolarFlux) {
            double[] solfluxFromL1b = new double[BAND_COUNT];
            for (int i = 0; i < BAND_COUNT; i++) {
                solfluxFromL1b[i] = sourceProduct.getBand("radiance_" + (i + 1)).getSolarFlux();
            }
            if (isSolfluxValid(solfluxFromL1b)) {
                algorithm.setSolflux(solfluxFromL1b);
            } else {
                throw new OperatorException("Invalid solar flux in source product!");
            }
        } else {
            solarFluxLazyLookup = new SolarFluxLazyLookup(DEFAULT_SOLAR_FLUX);
        }
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private static boolean isSolfluxValid(double[] solflux) {
        for (double v : solflux) {
            if (v <= 0.0) {
                return false;
            }
        }
        return true;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setUseDefaultSolarFlux(boolean useDefaultSolarFlux) {
        this.useDefaultSolarFlux = useDefaultSolarFlux;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccMerisOperator.class);
        }
    }
}
