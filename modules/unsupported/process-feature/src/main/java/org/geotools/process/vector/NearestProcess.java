/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2001-2007 TOPP - www.openplans.org.
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
package org.geotools.process.vector;

import java.util.logging.Logger;
import javax.measure.UnitConverter;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.measure.Measure;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;
import si.uom.SI;
import systems.uom.common.USCustomary;

@DescribeProcess(
        title = "Nearest Feature",
        description =
                "Returns the feature in a given feature collection that has the smallest distance to a given point.")
public class NearestProcess implements VectorProcess {
    private static final Logger LOGGER = Logging.getLogger(NearestProcess.class);

    /**
     * Process the input data set.
     *
     * @param featureCollection the data set
     * @param crs the CRS
     * @param point the given point
     * @return the snapped to feature
     * @throws ProcessException error
     */
    @DescribeResult(name = "result", description = "Nearest feature")
    public FeatureCollection execute(
            @DescribeParameter(name = "features", description = "Input feature collection")
                    FeatureCollection featureCollection,
            @DescribeParameter(name = "point", description = "Point from which to compute distance") Point point,
            @DescribeParameter(
                            name = "crs",
                            min = 0,
                            description =
                                    "Coordinate reference system of the collection and point (default is the input collection CRS)")
                    CoordinateReferenceSystem crs)
            throws ProcessException {
        try {
            if (crs == null) {
                GeometryDescriptor gd = featureCollection.getSchema().getGeometryDescriptor();
                if (gd != null) {
                    crs = gd.getCoordinateReferenceSystem();
                }
            }
            if (crs == null) {
                throw new ProcessException(
                        "The CRS parameter was not provided and the feature collection does not have a default one either");
            }

            CoordinateReferenceSystem epsg4326;
            try {
                epsg4326 = CRS.decode("EPSG:4326");
            } catch (Exception e) {
                throw new ProcessException("Unknown CRS code: EPSG:4326", e);
            }
            MathTransform crsTransform = CRS.findMathTransform(crs, epsg4326);

            DefaultFeatureCollection results = new DefaultFeatureCollection();
            FeatureType targetFeatureType = createTargetFeatureType(featureCollection.getSchema());
            UnitConverter unitConvert = SI.METRE.getConverterTo(USCustomary.MILE);
            Feature nearestFeature = null;
            double nearestDistance = 9e9;
            double nearestBearing = 0;
            try (FeatureIterator featureIterator = featureCollection.features()) {
                while (featureIterator.hasNext()) {
                    SimpleFeature f = (SimpleFeature) featureIterator.next();
                    if (f.getDefaultGeometryProperty().getValue() == null) continue;
                    DistanceOp op = new DistanceOp(
                            point, (Geometry) f.getDefaultGeometryProperty().getValue());
                    Coordinate[] co = op.nearestPoints();
                    double[] co0 = {
                        co[0].x, co[0].y,
                    };
                    double[] co1 = {
                        co[1].x, co[1].y,
                    };
                    double[] geo0 = new double[2];
                    double[] geo1 = new double[2];
                    crsTransform.transform(co0, 0, geo0, 0, 1);
                    crsTransform.transform(co1, 0, geo1, 0, 1);

                    // get distance
                    Measure m = DefaultGeographicCRS.WGS84.distance(geo0, geo1);
                    if (m.doubleValue() > nearestDistance) continue;
                    nearestFeature = f;
                    nearestDistance = m.doubleValue();
                    nearestBearing = calcBearing(co);
                }
            }
            if (nearestFeature != null) {
                nearestDistance = unitConvert.convert(nearestDistance);
                results.add(createTargetFeature(
                        nearestFeature, (SimpleFeatureType) targetFeatureType, nearestDistance, nearestBearing));
            }
            return results;
        } catch (ProcessException e) {
            throw e;
        } catch (Throwable e) {
            LOGGER.warning("Error executing method: " + e);
            throw new ProcessException("Error executing method: " + e, e);
        }
    }

    /**
     * Create the modified feature type.
     *
     * @param sourceFeatureType the source feature type
     * @return the modified feature type
     * @throws ProcessException errror
     */
    private SimpleFeatureType createTargetFeatureType(FeatureType sourceFeatureType) throws ProcessException {
        try {
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName(sourceFeatureType.getName().getLocalPart());
            typeBuilder.setNamespaceURI(sourceFeatureType.getName().getNamespaceURI());
            for (PropertyDescriptor attbType : sourceFeatureType.getDescriptors()) {
                typeBuilder.add((AttributeDescriptor) attbType);
            }
            typeBuilder.minOccurs(1).maxOccurs(1).nillable(false).add("nearest_distance", Double.class);
            typeBuilder.minOccurs(1).maxOccurs(1).nillable(false).add("nearest_bearing", Double.class);
            typeBuilder.setDefaultGeometry(
                    sourceFeatureType.getGeometryDescriptor().getLocalName());
            return typeBuilder.buildFeatureType();
        } catch (Exception e) {
            LOGGER.warning("Error creating type: " + e);
            throw new ProcessException("Error creating type: " + e, e);
        }
    }

    /**
     * Create the modified feature.
     *
     * @param feature the source feature
     * @param targetFeatureType the modified feature type
     * @param nearestDistance the snap distance
     * @param nearestBearing the snap bearing
     * @return the modified feature
     * @throws ProcessException error
     */
    private SimpleFeature createTargetFeature(
            Feature feature, SimpleFeatureType targetFeatureType, Double nearestDistance, Double nearestBearing)
            throws ProcessException {
        try {
            AttributeDescriptor distanceAttbType = targetFeatureType.getDescriptor("nearest_distance");
            AttributeDescriptor bearingAttbType = targetFeatureType.getDescriptor("nearest_bearing");
            Object[] attributes = new Object[targetFeatureType.getAttributeCount()];
            for (int i = 0; i < attributes.length; i++) {
                AttributeDescriptor attbType =
                        targetFeatureType.getAttributeDescriptors().get(i);
                if (attbType.equals(distanceAttbType)) {
                    attributes[i] = nearestDistance;
                } else if (attbType.equals(bearingAttbType)) {
                    attributes[i] = nearestBearing;
                } else {
                    attributes[i] = feature.getProperty(attbType.getName()).getValue();
                }
            }
            return SimpleFeatureBuilder.build(
                    targetFeatureType, attributes, feature.getIdentifier().getID());
        } catch (Exception e) {
            LOGGER.warning("Error creating feature: " + e);
            throw new ProcessException("Error creating feature: " + e, e);
        }
    }

    /**
     * Calculate the bearing between two points.
     *
     * @param coords the points
     * @return the bearing
     */
    private double calcBearing(Coordinate[] coords) {
        double y = Math.sin(coords[1].x - coords[0].x) * Math.cos(coords[1].y);
        double x = Math.cos(coords[0].y) * Math.sin(coords[1].y)
                - Math.sin(coords[0].y) * Math.cos(coords[1].y) * Math.cos(coords[1].x - coords[0].x);
        double brng = (Math.atan2(y, x) * 180.0 / Math.PI + 360) % 360;
        return brng;
    }
}
