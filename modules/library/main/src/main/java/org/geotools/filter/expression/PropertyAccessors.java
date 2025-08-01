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
package org.geotools.filter.expression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Convenience class for looking up a property accessor for a particular object type.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class PropertyAccessors {

    private static final Logger LOGGER = Logging.getLogger(PropertyAccessors.class);
    static final PropertyAccessorFactory[] FACTORY_CACHE;

    static {
        List<PropertyAccessorFactory> cache = new ArrayList<>();

        // add the simple feature property accessor factory first for performance
        // reasons
        cache.add(new NullPropertyAccessorFactory()); // NC - added
        cache.add(new SimpleFeaturePropertyAccessorFactory());
        cache.add(new DirectPropertyAccessorFactory());
        for (PropertyAccessorFactory factory : ServiceLoader.load(PropertyAccessorFactory.class)) {
            boolean nonCached = factory instanceof SimpleFeaturePropertyAccessorFactory
                    || factory instanceof DirectPropertyAccessorFactory
                    || factory instanceof NullPropertyAccessorFactory;
            if (!nonCached) {
                cache.add(factory);
            }
        }
        // sort the property accessors by their priority. The sort maintains the order of equal elements.
        cache.sort(Comparator.comparing(PropertyAccessorFactory::getPriority).reversed());
        if (LOGGER.isLoggable(Level.CONFIG)) {
            String log = cache.stream()
                    .filter(Objects::nonNull)
                    .map(factory -> {
                        String name = "[Name=" + factory.getClass().getSimpleName() + ", ";
                        return name + "Priority=" + factory.getPriority() + "]";
                    })
                    .collect(Collectors.joining("; "));
            LOGGER.config(log);
        }
        FACTORY_CACHE = cache.toArray(new PropertyAccessorFactory[cache.size()]);
    }

    /** Make sure this class won't be instantianted */
    private PropertyAccessors() {}

    /**
     * Looks up a list of {@link PropertyAccessor} for a particular object.
     *
     * <p>This method will return all accessors that is capable of handling the object and xpath expression provided, no
     * order is guaranteed.
     *
     * @param object The target object.
     * @param xpath An xpath expression denoting a property of the target object.
     * @param hints Hints to pass on to factories.
     * @return List of Property accessors, or <code>null</code> if object is null
     */
    public static List<PropertyAccessor> findPropertyAccessors(Object object, String xpath, Class target, Hints hints) {
        if (object == null) return null;

        List<PropertyAccessor> list = new ArrayList<>();

        for (PropertyAccessorFactory factory : FACTORY_CACHE) {
            PropertyAccessor accessor = factory.createPropertyAccessor(object.getClass(), xpath, target, hints);
            if (accessor != null && accessor.canHandle(object, xpath, target)) {
                list.add(accessor);
            }
        }
        return list;
    }
}
