/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
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

import org.geotools.api.feature.Attribute;
import org.geotools.util.factory.Hints;

public class ThisPropertyAccessorFactory implements PropertyAccessorFactory {

    static final ThisPropertyAccessor THIS_ACCESSOR = new ThisPropertyAccessor();

    @Override
    public PropertyAccessor createPropertyAccessor(Class type, String xpath, Class target, Hints hints) {
        if (".".equals(xpath)) return THIS_ACCESSOR;
        else return null;
    }

    static class ThisPropertyAccessor implements PropertyAccessor {

        @Override
        public boolean canHandle(Object object, String xpath, Class target) {
            return ".".equals(xpath);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object object, String xpath, Class<T> target) throws IllegalArgumentException {
            if (object instanceof Attribute) return (T) ((Attribute) object).getValue();
            else return (T) object;
        }

        @Override
        public void set(Object object, String xpath, Object value, Class target) throws IllegalArgumentException {
            throw new IllegalArgumentException("Can't change the value itself");
        }
    }
}
