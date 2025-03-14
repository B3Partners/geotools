/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.temporal.object;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.temporal.Duration;
import org.geotools.api.temporal.Instant;
import org.geotools.api.temporal.OrdinalReferenceSystem;
import org.geotools.api.temporal.Period;
import org.geotools.api.temporal.RelativePosition;
import org.geotools.api.temporal.Separation;
import org.geotools.api.temporal.TemporalGeometricPrimitive;

/**
 * An abstract class with two subclasses for representing a temporal instant and a temporal period.
 *
 * @author Mehdi Sidhoum (Geomatys)
 */
public abstract class DefaultTemporalGeometricPrimitive extends DefaultTemporalPrimitive
        implements TemporalGeometricPrimitive, Separation {

    /**
     * Returns the distance from this TM_GeometricPrimitive to another TM_GeometricPrimitive, i.e. the absolute value of
     * the difference between their temporal positions.
     */
    @Override
    public Duration distance(TemporalGeometricPrimitive other) {
        long diff = 0L;

        if (this instanceof Instant && other instanceof Instant) {
            if (((Instant) this).getPosition().anyOther() != null
                    && ((Instant) other).getPosition().anyOther() != null) {
                if (!((DefaultTemporalPosition) ((Instant) this).getPosition().anyOther())
                        .getFrame()
                        .equals(((DefaultTemporalPosition)
                                        ((Instant) other).getPosition().anyOther())
                                .getFrame())) {
                    try {
                        throw new Exception(
                                "the TM_TemporalPositions are not both associated with the same TM_ReferenceSystem !");
                    } catch (Exception ex) {
                        Logger.getLogger(DefaultTemporalGeometricPrimitive.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }
                }
            } else if (((Instant) this).getPosition().anyOther() != null) {
                if (((Instant) this).getPosition().anyOther().getIndeterminatePosition() != null
                        || ((DefaultTemporalPosition)
                                                ((Instant) this).getPosition().anyOther())
                                        .getFrame()
                                instanceof OrdinalReferenceSystem) {
                    try {
                        throw new Exception(
                                "either of the two TM_TemporalPositions is indeterminate or is associated with a TM_OrdianlReferenceSystem !");
                    } catch (Exception ex) {
                        Logger.getLogger(DefaultTemporalGeometricPrimitive.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }
                }
            } else if (((Instant) other).getPosition().anyOther() != null) {
                if (((Instant) other).getPosition().anyOther().getIndeterminatePosition() != null
                        || ((DefaultTemporalPosition)
                                                ((Instant) other).getPosition().anyOther())
                                        .getFrame()
                                instanceof OrdinalReferenceSystem) {
                    try {
                        throw new Exception(
                                "either of the two TM_TemporalPositions is indeterminate or is associated with a TM_OrdianlReferenceSystem !");
                    } catch (Exception ex) {
                        Logger.getLogger(DefaultTemporalGeometricPrimitive.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        if (this.relativePosition(other).equals(RelativePosition.BEFORE)
                || this.relativePosition(other).equals(RelativePosition.AFTER)) {
            if (this instanceof Instant && other instanceof Instant) {
                diff = Math.min(
                        Math.abs(iTime((Instant) other) - iTime((Instant) this)),
                        Math.abs(iTime((Instant) this) - iTime((Instant) other)));
            } else if (this instanceof Instant && other instanceof Period) {
                diff = Math.min(
                        Math.abs(pbTime((Period) other) - iTime((Instant) this)),
                        Math.abs(iTime(((Period) other).getEnding()) - iTime((Instant) this)));
            } else if (this instanceof Period && other instanceof Instant) {
                diff = Math.min(
                        Math.abs(iTime((Instant) other) - iTime(((Period) this).getEnding())),
                        Math.abs(iTime((Instant) other) - pbTime((Period) this)));
            } else if (this instanceof Period && other instanceof Period) {
                diff = Math.min(
                        Math.abs(peTime((Period) other) - pbTime((Period) this)),
                        Math.abs(pbTime((Period) other) - peTime((Period) this)));
            }
        } else if (this.relativePosition(other).equals(RelativePosition.BEGINS)
                || this.relativePosition(other).equals(RelativePosition.BEGUN_BY)
                || this.relativePosition(other).equals(RelativePosition.CONTAINS)
                || this.relativePosition(other).equals(RelativePosition.DURING)
                || this.relativePosition(other).equals(RelativePosition.ENDED_BY)
                || this.relativePosition(other).equals(RelativePosition.ENDS)
                || this.relativePosition(other).equals(RelativePosition.EQUALS)
                || this.relativePosition(other).equals(RelativePosition.MEETS)
                || this.relativePosition(other).equals(RelativePosition.MET_BY)
                || this.relativePosition(other).equals(RelativePosition.OVERLAPPED_BY)
                || this.relativePosition(other).equals(RelativePosition.OVERLAPS)) {
            diff = 0L;
        }

        Duration response = new DefaultPeriodDuration(Math.abs(diff));
        return response;
    }

    private long iTime(Instant instant) {
        return instant.getPosition().getDate().getTime();
    }

    private long peTime(Period other) {
        return iTime(other.getEnding());
    }

    private long pbTime(Period other) {
        return iTime(other.getBeginning());
    }

    /** Returns the length of this TM_GeometricPrimitive */
    @Override
    public Duration length() {
        Duration response = null;
        long diff = 0L;
        if (this instanceof Instant) {
            response = new DefaultPeriodDuration(Math.abs(diff));
            return response;
        } else {
            if (this instanceof Period) {
                if (((Period) this).getBeginning() != null && ((Period) this).getEnding() != null) {
                    response = ((Period) this).getBeginning().distance(((Period) this).getEnding());
                    return response;
                }
            }
            return null;
        }
    }
}
