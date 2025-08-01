/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.swing.tool;

import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.geometry.Position;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.util.XRectangle2D;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.parameter.Parameter;
import org.geotools.util.SuppressFBWarnings;

/**
 * Helper class used by {@linkplain InfoTool} to query values in a {@linkplain org.geotools.map.GridReaderLayer}.
 *
 * @author Michael Bedward
 * @since 8.0
 * @version $URL$
 */
public class GridReaderLayerHelper extends InfoToolHelper {

    private static final int CACHED_RASTER_WIDTH = 20;
    private WeakReference<GridCoverage2DReader> sourceRef;
    private GridCoverage2D cachedCoverage;

    public GridReaderLayerHelper() {}

    @Override
    public boolean isSupportedLayer(Layer layer) {
        return layer instanceof GridReaderLayer;
    }

    @Override
    public void setLayer(Layer layer) {
        if (!(layer instanceof GridReaderLayer)) {
            throw new IllegalArgumentException("layer must be an instance of GridReaderLayer");
        }

        super.setLayer(layer);
        sourceRef = new WeakReference<>(((GridReaderLayer) layer).getReader());
    }

    @Override
    public boolean isValid() {
        return super.isValid() && sourceRef != null && sourceRef.get() != null;
    }

    @Override
    public InfoToolResult getInfo(Position2D pos) throws Exception {
        InfoToolResult result = new InfoToolResult();

        if (isValid()) {
            Position trPos = InfoToolHelperUtils.getTransformed(pos, getContentToLayerTransform());

            if (cachedCoverage == null || !cachedCoverage.getEnvelope2D().contains(trPos)) {
                if (!renewCachedCoverage(trPos)) {
                    return result;
                }
            }

            try {
                Object objArray = cachedCoverage.evaluate(trPos);
                Number[] bandValues = InfoToolHelperUtils.asNumberArray(objArray);

                if (bandValues != null) {
                    result.newFeature("Raw values");
                    for (int i = 0; i < bandValues.length; i++) {
                        result.setFeatureValue("Band " + i, bandValues[i]);
                    }
                }

            } catch (PointOutsideCoverageException e) {
                // The empty result will be returned
            }
        }

        return result;
    }

    private boolean renewCachedCoverage(Position centrePos) {
        final Rectangle queryRect = createQueryGridEnvelope(centrePos);
        if (queryRect.isEmpty()) {
            return false;
        }

        final GridCoverage2DReader reader = sourceRef.get();
        if (reader == null) {
            return false;
        }
        GeneralParameterValue parameter = new Parameter<>(
                AbstractGridFormat.READ_GRIDGEOMETRY2D,
                new GridGeometry2D(
                        new GridEnvelope2D(queryRect),
                        reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                        reader.getCoordinateReferenceSystem()));

        try {
            cachedCoverage = reader.read(new GeneralParameterValue[] {parameter});
            return cachedCoverage != null;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE") // I just can't figure it out...
    private Rectangle createQueryGridEnvelope(Position pos) {
        final GridCoverage2DReader reader = sourceRef.get();
        if (reader == null) {
            throw new NullPointerException("Source refererence returned a null reader");
        }
        try {
            MathTransform worldToGridTransform =
                    reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER).inverse();

            Position midPos = worldToGridTransform.transform(pos, null);
            int x = (int) midPos.getOrdinate(0);
            int y = (int) midPos.getOrdinate(1);
            int halfWidth = CACHED_RASTER_WIDTH / 2;

            final Rectangle queryRect =
                    new Rectangle(x - halfWidth, y - halfWidth, CACHED_RASTER_WIDTH, CACHED_RASTER_WIDTH);

            GridEnvelope gridEnv = reader.getOriginalGridRange();
            Rectangle rect = new Rectangle(
                    gridEnv.getLow(0), gridEnv.getLow(1),
                    gridEnv.getSpan(0), gridEnv.getSpan(1));

            XRectangle2D.intersect(queryRect, rect, queryRect);
            return queryRect;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
