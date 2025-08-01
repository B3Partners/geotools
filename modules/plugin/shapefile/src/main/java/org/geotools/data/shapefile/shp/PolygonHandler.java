/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.shapefile.shp;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.coordinatesequence.CoordinateSequences;
import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Wrapper for a Shapefile polygon.
 *
 * @author aaime
 * @author Ian Schneider
 * @version $Id$
 */
public class PolygonHandler implements ShapeHandler {
    protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(PolygonHandler.class);

    GeometryFactory geometryFactory;

    final ShapeType shapeType;

    public PolygonHandler(GeometryFactory gf) {
        shapeType = ShapeType.POLYGON;
        this.geometryFactory = gf;
    }

    public PolygonHandler(ShapeType type, GeometryFactory gf) throws ShapefileException {
        if (!type.isPolygonType()) {
            throw new ShapefileException("PolygonHandler constructor - expected type to be 5, 15, or 25.");
        }

        shapeType = type;
        this.geometryFactory = gf;
    }

    // returns true if testPoint is a point in the pointList list.
    boolean pointInList(Coordinate testPoint, Coordinate[] pointList) {
        Coordinate p;

        for (int t = pointList.length - 1; t >= 0; t--) {
            p = pointList[t];

            // nan test; x!=x iff x is nan
            if (testPoint.x == p.x
                    && testPoint.y == p.y
                    && (testPoint.getZ() == p.getZ() || Double.isNaN(testPoint.getZ()))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ShapeType getShapeType() {
        return shapeType;
    }

    @Override
    public int getLength(Object geometry) {
        MultiPolygon multi;

        if (geometry instanceof MultiPolygon) {
            multi = (MultiPolygon) geometry;
        } else {
            multi = geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) geometry});
        }

        int nrings = 0;

        for (int t = 0; t < multi.getNumGeometries(); t++) {
            Polygon p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }

        int npoints = multi.getNumPoints();
        int length;

        if (shapeType == ShapeType.POLYGONZ) {
            length = 44 + 4 * nrings + 16 * npoints + 8 * npoints + 16 + 8 * npoints + 16;
        } else if (shapeType == ShapeType.POLYGONM) {
            length = 44 + 4 * nrings + 16 * npoints + 8 * npoints + 16;
        } else if (shapeType == ShapeType.POLYGON) {
            length = 44 + 4 * nrings + 16 * npoints;
        } else {
            throw new IllegalStateException("Expected ShapeType of Polygon, got " + shapeType);
        }
        return length;
    }

    @Override
    public Object read(ByteBuffer buffer, ShapeType type, boolean flatFeature) {
        if (type == ShapeType.NULL) {
            return createNull();
        }
        // bounds
        buffer.position(buffer.position() + 4 * 8);

        int numParts = buffer.getInt();
        int numPoints = buffer.getInt();
        int dimensions = shapeType == ShapeType.POLYGONZ && !flatFeature ? 3 : 2;

        int[] partOffsets = new int[numParts];

        for (int i = 0; i < numParts; i++) {
            partOffsets[i] = buffer.getInt();
        }

        ArrayList<LinearRing> shells = new ArrayList<>();
        ArrayList<LinearRing> holes = new ArrayList<>();
        CoordinateSequence coords = readCoordinates(buffer, numPoints, dimensions, flatFeature);

        int offset = 0;
        int start;
        int finish;
        int length;

        for (int part = 0; part < numParts; part++) {
            start = partOffsets[part];

            if (part == numParts - 1) {
                finish = numPoints;
            } else {
                finish = partOffsets[part + 1];
            }

            length = finish - start;
            int close = 0; // '1' if the ring must be closed, '0' otherwise
            if (coords.getOrdinate(start, CoordinateSequence.X) != coords.getOrdinate(finish - 1, CoordinateSequence.X)
                    || coords.getOrdinate(start, CoordinateSequence.Y)
                            != coords.getOrdinate(finish - 1, CoordinateSequence.Y)) {
                close = 1;
            }
            if (dimensions == 3 && !coords.hasM()) {
                if (coords.getOrdinate(start, CoordinateSequence.Z)
                        != coords.getOrdinate(finish - 1, CoordinateSequence.Z)) {
                    close = 1;
                }
            }

            CoordinateSequence csRing;

            if (coords.hasZ() && !flatFeature) {
                csRing = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), length + close, 4, 1);
            } else if (coords.hasM() && !flatFeature) {
                csRing = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), length + close, 3, 1);

            } else {
                csRing = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), length + close, 2);
            }

            // double area = 0;
            // int sx = offset;
            for (int i = 0; i < length; i++) {
                csRing.setOrdinate(i, CoordinateSequence.X, coords.getOrdinate(offset, CoordinateSequence.X));
                csRing.setOrdinate(i, CoordinateSequence.Y, coords.getOrdinate(offset, CoordinateSequence.Y));
                if (coords.hasZ() && !flatFeature) {
                    csRing.setOrdinate(i, CoordinateSequence.Z, coords.getOrdinate(offset, CoordinateSequence.Z));
                }
                if (coords.hasM() && !flatFeature) {
                    double m = coords.getOrdinate(offset, CoordinateSequence.M);
                    if (m < -10e38) {
                        m = Double.NaN;
                    }
                    csRing.setOrdinate(i, CoordinateSequence.M, m);
                }
                offset++;
            }
            if (close == 1) {
                csRing.setOrdinate(length, CoordinateSequence.X, coords.getOrdinate(start, CoordinateSequence.X));
                csRing.setOrdinate(length, CoordinateSequence.Y, coords.getOrdinate(start, CoordinateSequence.Y));
                if (coords.hasZ() && !flatFeature) {
                    csRing.setOrdinate(length, CoordinateSequence.Z, coords.getOrdinate(start, CoordinateSequence.Z));
                }
                if (coords.hasM() && !flatFeature) {
                    csRing.setOrdinate(length, CoordinateSequence.M, coords.getOrdinate(start, CoordinateSequence.M));
                }
            }
            // REVISIT: polygons with only 1 or 2 points are not polygons -
            // geometryFactory will bomb so we skip if we find one.
            if (csRing.size() == 0 || csRing.size() > 3) {
                LinearRing ring = geometryFactory.createLinearRing(csRing);

                if (CoordinateSequences.isCCW(csRing)) {
                    // counter-clockwise
                    holes.add(ring);
                } else {
                    // clockwise
                    shells.add(ring);
                }
            }
        }

        // quick optimization: if there's only one shell no need to check
        // for holes inclusion
        if (shells.size() == 1) {
            return createMulti(shells.get(0), holes);
        }
        // if for some reason, there is only one hole, we just reverse it and
        // carry on.
        else if (holes.size() == 1 && shells.isEmpty()) {
            return createMulti(holes.get(0));
        } else {

            // build an association between shells and holes
            final List<List<LinearRing>> holesForShells = assignHolesToShells(shells, holes);

            Geometry g = buildGeometries(shells, holes, holesForShells);

            return g;
        }
    }

    /** @param flatFeature */
    private CoordinateSequence readCoordinates(
            final ByteBuffer buffer, final int numPoints, final int dimensions, boolean flatFeature) {
        CoordinateSequence cs;
        if (shapeType == ShapeType.POLYGONM && !flatFeature) {
            cs = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), numPoints, 3, 1);
        } else if (shapeType == ShapeType.POLYGONZ && !flatFeature) {
            cs = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), numPoints, 4, 1);
        } else {
            cs = JTS.createCS(geometryFactory.getCoordinateSequenceFactory(), numPoints, dimensions);
        }
        DoubleBuffer dbuffer = buffer.asDoubleBuffer();
        double[] ordinates = new double[numPoints * 2];
        dbuffer.get(ordinates);
        for (int t = 0; t < numPoints; t++) {
            cs.setOrdinate(t, CoordinateSequence.X, ordinates[t * 2]);
            cs.setOrdinate(t, CoordinateSequence.Y, ordinates[t * 2 + 1]);
        }
        if (!flatFeature) {
            if (shapeType == ShapeType.POLYGONZ) { // Handle Z
                dbuffer.position(dbuffer.position() + 2);
                dbuffer.get(ordinates, 0, numPoints);

                for (int t = 0; t < numPoints; t++) {
                    cs.setOrdinate(t, CoordinateSequence.Z, ordinates[t]);
                }
            }

            boolean isArcZWithM = dbuffer.remaining() >= numPoints + 2 && shapeType == ShapeType.POLYGONZ;
            if (isArcZWithM || shapeType == ShapeType.POLYGONM) {
                // Handle M
                dbuffer.position(dbuffer.position() + 2);
                dbuffer.get(ordinates, 0, numPoints);

                for (int t = 0; t < numPoints; t++) {
                    cs.setOrdinate(t, CoordinateSequence.M, ordinates[t]);
                }
            }
        }
        return cs;
    }

    /** */
    private Geometry buildGeometries(
            final List<LinearRing> shells, final List<LinearRing> holes, final List<List<LinearRing>> holesForShells) {
        Polygon[] polygons;

        // if we have shells, lets use them
        if (shells.isEmpty()) {
            // oh, this is a bad record with only holes
            polygons = new Polygon[holes.size()];
        } else {
            polygons = new Polygon[shells.size()];
        }

        // this will do nothing for the "only holes case"
        for (int i = 0; i < shells.size(); i++) {
            LinearRing shell = shells.get(i);
            List<LinearRing> holesForShell = holesForShells.get(i);
            polygons[i] =
                    geometryFactory.createPolygon(shell, holesForShell.toArray(new LinearRing[holesForShell.size()]));
        }

        // this will take care of the "only holes case"
        // we just reverse each hole
        if (shells.isEmpty()) {
            for (int i = 0, ii = holes.size(); i < ii; i++) {
                LinearRing hole = holes.get(i);
                polygons[i] = geometryFactory.createPolygon(hole, null);
            }
        }

        Geometry g = geometryFactory.createMultiPolygon(polygons);

        return g;
    }

    /** <b>Package private for testing</b> */
    List<List<LinearRing>> assignHolesToShells(final ArrayList<LinearRing> shells, final ArrayList<LinearRing> holes) {
        List<List<LinearRing>> holesForShells = getListOfLists(shells.size());

        // find homes
        for (LinearRing testRing : holes) {
            LinearRing minShell = null;
            Envelope minEnv = null;
            Envelope testEnv = testRing.getEnvelopeInternal();
            Coordinate testPt = testRing.getCoordinateN(0);
            LinearRing tryRing;

            for (LinearRing shell : shells) {
                tryRing = shell;

                Envelope tryEnv = tryRing.getEnvelopeInternal();
                if (minShell != null) {
                    minEnv = minShell.getEnvelopeInternal();
                }

                boolean isContained = false;
                Coordinate[] coordList = tryRing.getCoordinates();

                if (tryEnv.contains(testEnv)
                        && (RayCrossingCounter.locatePointInRing(testPt, coordList) != 2
                                || pointInList(testPt, coordList))) {
                    isContained = true;
                }

                // check if this new containing ring is smaller than the current
                // minimum ring
                if (isContained) {
                    if (minShell == null || minEnv.contains(tryEnv)) {
                        minShell = tryRing;
                    }
                }
            }

            if (minShell == null) {
                // now reverse this bad "hole" and turn it into a shell
                shells.add(testRing);
                holesForShells.add(new ArrayList<>());
            } else {
                holesForShells.get(shells.indexOf(minShell)).add(testRing);
            }
        }

        return holesForShells;
    }

    private List<List<LinearRing>> getListOfLists(int size) {
        List<List<LinearRing>> holesForShells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            holesForShells.add(new ArrayList<>());
        }
        return holesForShells;
    }

    private MultiPolygon createMulti(LinearRing single) {
        return createMulti(single, java.util.Collections.emptyList());
    }

    private MultiPolygon createMulti(LinearRing single, List<LinearRing> holes) {
        return geometryFactory.createMultiPolygon(
                new Polygon[] {geometryFactory.createPolygon(single, holes.toArray(new LinearRing[holes.size()]))});
    }

    private MultiPolygon createNull() {
        return geometryFactory.createMultiPolygon(null);
    }

    @Override
    public void write(ByteBuffer buffer, Object geometry) {
        MultiPolygon multi;

        if (geometry instanceof MultiPolygon) {
            multi = (MultiPolygon) geometry;
        } else {
            multi = geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) geometry});
        }

        Envelope box = multi.getEnvelopeInternal();
        buffer.putDouble(box.getMinX());
        buffer.putDouble(box.getMinY());
        buffer.putDouble(box.getMaxX());
        buffer.putDouble(box.getMaxY());

        // need to find the total number of rings and points
        final int nrings;
        final CoordinateSequence[] coordinates;
        {
            List<CoordinateSequence> allCoords = new ArrayList<>();
            for (int t = 0; t < multi.getNumGeometries(); t++) {
                Polygon p = (Polygon) multi.getGeometryN(t);
                allCoords.add(p.getExteriorRing().getCoordinateSequence());
                for (int ringN = 0; ringN < p.getNumInteriorRing(); ringN++) {
                    allCoords.add(p.getInteriorRingN(ringN).getCoordinateSequence());
                }
            }
            coordinates = allCoords.toArray(new CoordinateSequence[allCoords.size()]);
            nrings = coordinates.length;
        }

        final int npoints = multi.getNumPoints();

        buffer.putInt(nrings);
        buffer.putInt(npoints);

        int count = 0;
        for (int t = 0; t < nrings; t++) {
            buffer.putInt(count);
            count = count + coordinates[t].size();
        }

        final double[] zExtreame = {Double.NaN, Double.NaN};

        // write out points here!.. and gather up min and max z values
        for (int ringN = 0; ringN < nrings; ringN++) {
            CoordinateSequence coords = coordinates[ringN];

            JTSUtilities.zMinMax(coords, zExtreame);

            final int seqSize = coords.size();
            for (int coordN = 0; coordN < seqSize; coordN++) {
                buffer.putDouble(coords.getOrdinate(coordN, 0));
                buffer.putDouble(coords.getOrdinate(coordN, 1));
            }
        }

        if (shapeType == ShapeType.POLYGONZ) {
            // z
            if (Double.isNaN(zExtreame[0])) {
                buffer.putDouble(0.0);
                buffer.putDouble(0.0);
            } else {
                buffer.putDouble(zExtreame[0]);
                buffer.putDouble(zExtreame[1]);
            }

            for (int ringN = 0; ringN < nrings; ringN++) {
                CoordinateSequence coords = coordinates[ringN];

                final int seqSize = coords.size();
                double z;
                for (int coordN = 0; coordN < seqSize; coordN++) {
                    z = coords.getOrdinate(coordN, 2);
                    if (Double.isNaN(z)) {
                        buffer.putDouble(0.0);
                    } else {
                        buffer.putDouble(z);
                    }
                }
            }
        }

        if (shapeType == ShapeType.POLYGONM || shapeType == ShapeType.POLYGONZ) {
            // obtain all M values
            List<Double> values = new ArrayList<>();
            for (int ringN = 0; ringN < nrings; ringN++) {
                CoordinateSequence coords = coordinates[ringN];
                final int seqSize = coords.size();
                double m;
                for (int coordN = 0; coordN < seqSize; coordN++) {
                    m = coords.getM(coordN);
                    values.add(m);
                }
            }

            // m min
            double edge = values.stream().min(Double::compare).get();
            buffer.putDouble(!Double.isNaN(edge) ? edge : -10E40);
            // m max
            edge = values.stream().max(Double::compare).get();
            buffer.putDouble(!Double.isNaN(edge) ? edge : -10E40);

            // m values
            values.forEach(x -> {
                buffer.putDouble(Double.isNaN(x) ? -10E40 : x);
            });
        }
    }
}

/*
 * $Log: PolygonHandler.java,v $ Revision 1.9 2004/02/17 18:10:23 ianschneider changed to use GeometryFactory for Geometry creation
 *
 * Revision 1.8 2003/07/24 19:10:02 ianschneider *** empty log message ***
 *
 * Revision 1.7 2003/07/24 18:32:10 ianschneider more test updates, fixed Z type writing
 *
 * Revision 1.6 2003/07/23 23:41:09 ianschneider more testing updates
 *
 * Revision 1.5 2003/07/23 00:59:59 ianschneider Lots of PMD fix ups
 *
 * Revision 1.4 2003/07/21 21:15:29 jmacgill small fix for shapefiles with an invalid hole (only 1 or 2 points)
 *
 * Revision 1.3 2003/05/19 21:38:55 jmacgill refactored read method to break it up a little
 *
 * Revision 1.2 2003/05/19 20:51:30 ianschneider removed System.out print statements
 *
 * Revision 1.1 2003/05/14 17:51:21 ianschneider migrated packages
 *
 * Revision 1.3 2003/04/30 23:19:46 ianschneider Added construction of multi geometries for default return values, even if only one geometry. This
 * could have effects through system.
 *
 * Revision 1.2 2003/03/30 20:21:09 ianschneider Moved buffer branch to main
 *
 * Revision 1.1.2.5 2003/03/29 22:30:09 ianschneider For case of hole without shell - reverse hole, add to shell list
 *
 * Revision 1.1.2.4 2003/03/26 19:30:30 ianschneider Made hack to reverse polygon records if they contains only holes
 *
 * Revision 1.1.2.3 2003/03/12 15:30:18 ianschneider made ShapeType final for handlers - once they're created, it won't change.
 *
 * Revision 1.1.2.2 2003/03/07 00:36:41 ianschneider
 *
 * Added back the additional ShapeType parameter in ShapeHandler.read. ShapeHandler's need return their own special "null" shape if needed. Fixed the
 * ShapefileReader to not throw exceptions for "null" shapes. Fixed ShapefileReader to accomodate junk after the last valid record. The theory goes,
 * if the shape number is proper, that is, one greater than the previous, we consider that a valid record and attempt to read it. I suppose, by
 * chance, the junk could coincide with the next record number. Stupid ESRI. Fixed some record-length calculations which resulted in writing of bad
 * shapefiles.
 *
 * Revision 1.1.2.1 2003/03/06 01:16:34 ianschneider
 *
 * The initial changes for moving to java.nio. Added some documentation and improved exception handling. Works for reading, may work for writing as of
 * now.
 *
 * Revision 1.1 2003/02/27 22:35:50 aaime New shapefile module, initial commit
 *
 * Revision 1.2 2003/01/22 18:31:05 jaquino Enh: Make About Box configurable
 *
 * Revision 1.2 2002/09/09 20:46:22 dblasby Removed LEDatastream refs and replaced with EndianData[in/out]putstream
 *
 * Revision 1.1 2002/08/27 21:04:58 dblasby orginal
 *
 * Revision 1.3 2002/03/05 10:51:01 andyt removed use of factory from write method
 *
 * Revision 1.2 2002/03/05 10:23:59 jmacgill made sure geometries were created using the factory methods
 *
 * Revision 1.1 2002/02/28 00:38:50 jmacgill Renamed files to more intuitve names
 *
 * Revision 1.4 2002/02/13 00:23:53 jmacgill First semi working JTS version of Shapefile code
 *
 * Revision 1.3 2002/02/11 18:44:22 jmacgill replaced geometry constructions with calls to geometryFactory.createX methods
 *
 * Revision 1.2 2002/02/11 18:28:41 jmacgill rewrote to have static read and write methods
 *
 * Revision 1.1 2002/02/11 16:54:43 jmacgill added shapefile code and directories
 *
 */
