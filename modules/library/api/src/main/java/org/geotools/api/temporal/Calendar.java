/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005, Open Geospatial Consortium Inc.
 *
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.geotools.api.temporal;

import java.util.Collection;

/**
 * A discrete temporal reference system that provides a basis for defining temporal position to a resolution of one day.
 *
 * @author Alexander Petkov
 */
public interface Calendar extends TemporalReferenceSystem {
    /** Converts a {@linkplain CalendarDate date} in this calendar to a {@linkplain JulianDate julian date}. */
    JulianDate dateTrans(CalendarDate date, ClockTime time);

    /** Converts a {@linkplain JulianDate julian date} to a {@linkplain CalendarDate date} in this calendar. */
    CalendarDate julTrans(JulianDate julian);

    /**
     * links this calendar to the {@linkplain CalendarEra calendar eras} that it uses as a reference for dating.
     *
     * @todo The original version of this class returned {@code TemporalCalendarEra}, which doesn't exists in the
     *     provided sources. I assumed that it was a typo and that the actual class was {@link CalendarEra}.
     */
    Collection<CalendarEra> getBasis();

    /**
     * Links this calendar to the {@linkplain Clock clock} that is used for specifying temporal positions within the
     * smallest calendar interval.
     *
     * @todo Method name doesn't match the UML identifier.
     */
    Clock getClock();
}
