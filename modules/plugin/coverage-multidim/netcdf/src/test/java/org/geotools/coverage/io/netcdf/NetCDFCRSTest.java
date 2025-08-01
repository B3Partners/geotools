/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015 - 2016, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverage.io.netcdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.measure.Unit;
import org.apache.commons.io.FileUtils;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.DerivedCRS;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.datum.Ellipsoid;
import org.geotools.api.referencing.datum.GeodeticDatum;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.Projection;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.io.netcdf.crs.NetCDFCRSAuthorityFactory;
import org.geotools.coverage.io.netcdf.crs.NetCDFCoordinateReferenceSystemType;
import org.geotools.coverage.io.netcdf.crs.NetCDFProjection;
import org.geotools.coverage.io.netcdf.crs.ProjectionBuilder;
import org.geotools.geometry.GeneralBounds;
import org.geotools.imageio.netcdf.utilities.NetCDFCRSUtilities;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefiningConversion;
import org.geotools.referencing.operation.projection.AlbersEqualArea;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.RotatedPole;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.geotools.test.TestData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testing NetCDF Projection management machinery
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class NetCDFCRSTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String CUSTOM_EPSG_KM = "970917";
    private static final String CUSTOM_EPSG_M = "970916";
    private static final double COORDINATE_IN_METERS = 2.5E6;
    private static final double COORDINATE_IN_KM = COORDINATE_IN_METERS / 1000;

    private static final double DELTA = 1E-6;

    private static CoordinateReferenceSystem UTM32611;

    /** Sets up the custom definitions */
    @BeforeClass
    public static void setUpClass() throws Exception {
        String netcdfPropertiesPath = TestData.file(NetCDFCRSTest.class, "netcdf.projections.properties")
                .getCanonicalPath();
        System.setProperty(NetCDFCRSAuthorityFactory.SYSTEM_DEFAULT_USER_PROJ_FILE, netcdfPropertiesPath);
        CRS.reset("all");
        UTM32611 = CRS.decode("EPSG:32611");
    }

    @AfterClass
    public static void cleanUp() {
        System.clearProperty(NetCDFCRSAuthorityFactory.SYSTEM_DEFAULT_USER_PROJ_FILE);
    }

    @Test
    public void testUTMDatasetSpatialRef() throws Exception {
        final File file = TestData.file(this, "utm_spatial_ref.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            GeographicCRS baseCRS = projectedCRS.getBaseCRS();

            // Dealing with SPATIAL_REF Attribute
            assertTrue(CRS.equalsIgnoreMetadata(baseCRS, DefaultGeographicCRS.WGS84));
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            assertTrue(transform instanceof TransverseMercator);
            MathTransform sourceToTargetTransform = CRS.findMathTransform(crs, UTM32611);
            assertTrue(sourceToTargetTransform.isIdentity());
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testUTMDatasetNoCode() throws Exception {
        final File file = TestData.file(this, "utmnocode.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            assertTrue(transform instanceof TransverseMercator);

            // Check the proper CRS Type has been recognized
            NetCDFCoordinateReferenceSystemType crsType = NetCDFCoordinateReferenceSystemType.parseCRS(crs);
            assertSame(NetCDFCoordinateReferenceSystemType.TRANSVERSE_MERCATOR, crsType);
            assertSame(NetCDFCoordinateReferenceSystemType.NetCDFCoordinate.YX_COORDS, crsType.getCoordinates());
            assertSame(NetCDFProjection.TRANSVERSE_MERCATOR, crsType.getNetCDFProjection());
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testEsriPeStringReferencing() throws Exception {
        final File file = TestData.file(this, "utm_esri_pe_string.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            MathTransform sourceToTargetTransform = CRS.findMathTransform(crs, UTM32611);
            assertTrue(sourceToTargetTransform.isIdentity());
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testAlbersEqualAreaDataset() throws Exception {
        final File file = TestData.file(this, "albersequal.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            assertTrue(transform instanceof AlbersEqualArea);

            // Check the proper CRS Type has been recognized
            NetCDFCoordinateReferenceSystemType crsType = NetCDFCoordinateReferenceSystemType.parseCRS(crs);
            assertSame(NetCDFCoordinateReferenceSystemType.ALBERS_EQUAL_AREA, crsType);
            assertSame(NetCDFCoordinateReferenceSystemType.NetCDFCoordinate.YX_COORDS, crsType.getCoordinates());
            assertSame(NetCDFProjection.ALBERS_EQUAL_AREA, crsType.getNetCDFProjection());
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testRotatedPoleDataset() throws Exception {
        final File file = TestData.file(this, "rotated-pole.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            NetCDFCoordinateReferenceSystemType crsType = NetCDFCoordinateReferenceSystemType.parseCRS(crs);
            assertSame(NetCDFCoordinateReferenceSystemType.ROTATED_POLE, crsType);
            assertSame(NetCDFCoordinateReferenceSystemType.NetCDFCoordinate.RLATLON_COORDS, crsType.getCoordinates());
            assertSame(NetCDFProjection.ROTATED_POLE, crsType.getNetCDFProjection());
            assertTrue(crs instanceof DerivedCRS);
            DerivedCRS derivedCRS = (DerivedCRS) crs;
            MathTransform transform = derivedCRS.getConversionFromBase().getMathTransform();
            assertTrue(transform instanceof RotatedPole);
            RotatedPole rotatedPole = (RotatedPole) transform;
            ParameterValueGroup values = rotatedPole.getParameterValues();
            assertEquals(
                    -106.0, values.parameter(NetCDFUtilities.CENTRAL_MERIDIAN).doubleValue(), DELTA);
            assertEquals(
                    54.0, values.parameter(NetCDFUtilities.LATITUDE_OF_ORIGIN).doubleValue(), DELTA);

            String wkt = "FITTED_CS[\"rotated_latitude_longitude\", \n"
                    + "  INVERSE_MT[PARAM_MT[\"Rotated_Pole\", \n"
                    + "      PARAMETER[\"semi_major\", 6371229.0], \n"
                    + "      PARAMETER[\"semi_minor\", 6371229.0], \n"
                    + "      PARAMETER[\"central_meridian\", -106.0], \n"
                    + "      PARAMETER[\"latitude_of_origin\", 54.0], \n"
                    + "      PARAMETER[\"scale_factor\", 1.0], \n"
                    + "      PARAMETER[\"false_easting\", 0.0], \n"
                    + "      PARAMETER[\"false_northing\", 0.0]]], \n"
                    + "  GEOGCS[\"unknown\", \n"
                    + "    DATUM[\"unknown\", \n"
                    + "      SPHEROID[\"unknown\", 6371229.0, 0.0]], \n"
                    + "    PRIMEM[\"Greenwich\", 0.0], \n"
                    + "    UNIT[\"degree\", 0.017453292519943295], \n"
                    + "    AXIS[\"Geodetic longitude\", EAST], \n"
                    + "    AXIS[\"Geodetic latitude\", NORTH]], \n"
                    + "  AUTHORITY[\"EPSG\",\"971806\"]]";
            CoordinateReferenceSystem wktCRS = CRS.parseWKT(wkt);
            assertTrue(CRS.equalsIgnoreMetadata(wktCRS, crs));
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    @Test
    public void testCachedUnits() throws Exception {
        final File file = TestData.file(this, "rotated-pole.nc");
        NetCDFReader reader = null;
        Unit<?> unit1 = null;
        Unit<?> unit2 = null;
        try {
            reader = new NetCDFReader(file, null);
            GridCoverage2D coverage = reader.read();
            GridSampleDimension sampleDimension = coverage.getSampleDimension(0);
            unit1 = sampleDimension.getUnits();
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
        try {
            reader = new NetCDFReader(file, null);
            GridCoverage2D coverage = reader.read();
            GridSampleDimension sampleDimension = coverage.getSampleDimension(0);
            unit2 = sampleDimension.getUnits();
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
        assertSame(unit1, unit2);
    }

    @Test
    public void testProjectionSetup() throws Exception {
        ParameterValueGroup params =
                ProjectionBuilder.getProjectionParameters(NetCDFProjection.LAMBERT_CONFORMAL_CONIC_1SP.getOGCName());
        params.parameter("central_meridian").setValue(-95.0);
        params.parameter("latitude_of_origin").setValue(25.0);
        params.parameter("scale_factor").setValue(1.0);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);

        Map<String, Number> ellipsoidParams = new HashMap<>();
        ellipsoidParams.put(NetCDFUtilities.SEMI_MAJOR, 6378137);
        ellipsoidParams.put(NetCDFUtilities.INVERSE_FLATTENING, 298.257223563);

        Ellipsoid ellipsoid = ProjectionBuilder.createEllipsoid("WGS 84", ellipsoidParams);
        ProjectionBuilder.updateEllipsoidParams(params, ellipsoid);
        GeodeticDatum datum = ProjectionBuilder.createGeodeticDatum("WGS_1984", ellipsoid);
        GeographicCRS geoCRS = ProjectionBuilder.createGeographicCRS("WGS 84", datum);
        MathTransform transform = ProjectionBuilder.createTransform(params);
        DefiningConversion conversionFromBase =
                ProjectionBuilder.createConversionFromBase("lambert_conformal_mercator_1sp", transform);

        CoordinateReferenceSystem crs = ProjectionBuilder.createProjectedCRS(
                Collections.singletonMap("name", "custom_lambert_conformal_conic_1sp"),
                geoCRS,
                conversionFromBase,
                transform);

        assertTrue(crs instanceof ProjectedCRS);
        ProjectedCRS projectedCRS = (ProjectedCRS) crs;
        GeographicCRS baseCRS = projectedCRS.getBaseCRS();

        assertTrue(CRS.equalsIgnoreMetadata(baseCRS, DefaultGeographicCRS.WGS84));
        assertTrue(transform instanceof LambertConformal1SP);
    }

    @Test
    public void testDefaultDatumSetup() throws Exception {
        ParameterValueGroup params =
                ProjectionBuilder.getProjectionParameters(NetCDFProjection.LAMBERT_CONFORMAL_CONIC_1SP.getOGCName());
        params.parameter("central_meridian").setValue(-95.0);
        params.parameter("latitude_of_origin").setValue(25.0);
        params.parameter("scale_factor").setValue(1.0);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);

        // Intentionally left empty
        Map<String, Number> ellipsoidParams = new HashMap<>();

        Ellipsoid ellipsoid = ProjectionBuilder.createEllipsoid("Unknown", ellipsoidParams);
        ProjectionBuilder.updateEllipsoidParams(params, ellipsoid);
        assertEquals(NetCDFUtilities.DEFAULT_EARTH_RADIUS, ellipsoid.getSemiMajorAxis(), DELTA);
        assertEquals(NetCDFUtilities.DEFAULT_EARTH_RADIUS, ellipsoid.getSemiMinorAxis(), DELTA);
        assertTrue(Double.isInfinite(ellipsoid.getInverseFlattening()));
    }

    @Test
    public void testMultipleBoundingBoxesSupport() throws IOException, FactoryException {
        final File testURL = TestData.file(this, "dualbbox.nc");

        final NetCDFReader reader = new NetCDFReader(testURL, null);
        assertNotNull(reader);
        try {
            String[] names = reader.getGridCoverageNames();
            assertNotNull(names);
            assertEquals(2, names.length);
            assertEquals(names[0], "sample1");
            assertEquals(names[1], "sample2");

            GeneralBounds envelope1 = reader.getOriginalEnvelope("sample1");
            GeneralBounds envelope2 = reader.getOriginalEnvelope("sample2");

            // Check the envelopes are different
            assertEquals(52000, envelope1.getMinimum(0), DELTA);
            assertEquals(-78000, envelope1.getMinimum(1), DELTA);
            assertEquals(68000, envelope1.getMaximum(0), DELTA);
            assertEquals(-62000, envelope1.getMaximum(1), DELTA);

            assertEquals(-58000, envelope2.getMinimum(0), DELTA);
            assertEquals(0, envelope2.getMinimum(1), DELTA);
            assertEquals(-46000, envelope2.getMaximum(0), DELTA);
            assertEquals(12000, envelope2.getMaximum(1), DELTA);

            // Check the envelopes have different span
            assertEquals(16000, envelope1.getSpan(0), DELTA);
            assertEquals(16000, envelope1.getSpan(1), DELTA);
            assertEquals(12000, envelope2.getSpan(0), DELTA);
            assertEquals(12000, envelope2.getSpan(1), DELTA);

            // Double check on gridRanges
            GridEnvelope gridRange1 = reader.getOriginalGridRange("sample1");
            GridEnvelope gridRange2 = reader.getOriginalGridRange("sample2");

            // Check the samples are 4x4 and 3x3 respectively
            assertEquals(4, gridRange1.getSpan(0));
            assertEquals(4, gridRange1.getSpan(1));
            assertEquals(3, gridRange2.getSpan(0));
            assertEquals(3, gridRange2.getSpan(1));

        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testMultipleBoundingBoxesAuxiliaryCoordinatesSupport() throws IOException, FactoryException {
        final File testURL = TestData.file(this, "dualbboxAuxiliaryCoordinates.nc");

        final NetCDFReader reader = new NetCDFReader(testURL, null);
        assertNotNull(reader);
        try {
            String[] names = reader.getGridCoverageNames();
            assertNotNull(names);
            assertEquals(2, names.length);
            assertEquals(names[0], "sample1");
            assertEquals(names[1], "sample2");

            GeneralBounds envelope1 = reader.getOriginalEnvelope("sample1");
            GeneralBounds envelope2 = reader.getOriginalEnvelope("sample2");

            // Check the envelopes are different
            assertEquals(52000, envelope1.getMinimum(0), DELTA);
            assertEquals(-78000, envelope1.getMinimum(1), DELTA);
            assertEquals(68000, envelope1.getMaximum(0), DELTA);
            assertEquals(-62000, envelope1.getMaximum(1), DELTA);

            assertEquals(-58000, envelope2.getMinimum(0), DELTA);
            assertEquals(0, envelope2.getMinimum(1), DELTA);
            assertEquals(-46000, envelope2.getMaximum(0), DELTA);
            assertEquals(12000, envelope2.getMaximum(1), DELTA);

            // Check the envelopes have different span
            assertEquals(16000, envelope1.getSpan(0), DELTA);
            assertEquals(16000, envelope1.getSpan(1), DELTA);
            assertEquals(12000, envelope2.getSpan(0), DELTA);
            assertEquals(12000, envelope2.getSpan(1), DELTA);

            // Double check on gridRanges
            GridEnvelope gridRange1 = reader.getOriginalGridRange("sample1");
            GridEnvelope gridRange2 = reader.getOriginalGridRange("sample2");

            // Check the samples are 4x4 and 3x3 respectively
            assertEquals(4, gridRange1.getSpan(0));
            assertEquals(4, gridRange1.getSpan(1));
            assertEquals(3, gridRange2.getSpan(0));
            assertEquals(3, gridRange2.getSpan(1));

        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    /** Test that an unsupported grid_mapping_name falls back to WGS 84. */
    @Test
    public void testUnsupportedGridMappingName() throws IOException {
        File file = TestData.file(this, "unsupported-grid-mapping-name.nc");
        NetCDFReader reader = new NetCDFReader(file, null);
        assertNotNull(reader);
        assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84));
        assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem("foo"), DefaultGeographicCRS.WGS84));
        reader.dispose();
    }

    @Test
    public void testPreserveKM() throws Exception {
        NetCDFCRSUtilities.setConvertAxisKm(false);
        String fileName = "samplekm.nc";
        File nc1 = TestData.file(this, fileName);
        File converted = tempFolder.newFolder("converted");
        FileUtils.copyFileToDirectory(nc1, converted);
        File km = new File(converted, fileName);
        NetCDFReader reader = new NetCDFReader(km, null);
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(reader.getGridCoverageNames()[0]);
        assertEquals(CUSTOM_EPSG_KM, crs.getIdentifiers().iterator().next().getCode());
        assertTrue("km"
                .equalsIgnoreCase(crs.getCoordinateSystem().getAxis(0).getUnit().toString()));
        GeneralBounds originalEnvelope = reader.getOriginalEnvelope(reader.getGridCoverageNames()[0]);
        assertEquals(COORDINATE_IN_KM, originalEnvelope.getMaximum(0), DELTA);
    }

    @Test
    public void testAutoConversionKmToM() throws Exception {
        NetCDFCRSUtilities.setConvertAxisKm(true);
        String fileName = "samplekm.nc";
        File nc1 = TestData.file(this, fileName);
        File converted = tempFolder.newFolder("converted");
        FileUtils.copyFileToDirectory(nc1, converted);
        File m = new File(converted, fileName);
        NetCDFReader reader = new NetCDFReader(m, null);
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(reader.getGridCoverageNames()[0]);
        assertEquals(CUSTOM_EPSG_M, crs.getIdentifiers().iterator().next().getCode());
        assertTrue("m"
                .equalsIgnoreCase(crs.getCoordinateSystem().getAxis(0).getUnit().toString()));
        GeneralBounds originalEnvelope = reader.getOriginalEnvelope(reader.getGridCoverageNames()[0]);
        assertEquals(COORDINATE_IN_METERS, originalEnvelope.getMaximum(0), DELTA);
    }

    /** Cleanup the custom definitions */
    @After
    public void cleanUpDefinitions() throws Exception {
        System.clearProperty(NetCDFCRSAuthorityFactory.SYSTEM_DEFAULT_USER_PROJ_FILE);
        NetCDFCRSUtilities.setConvertAxisKm(false);
    }
}
