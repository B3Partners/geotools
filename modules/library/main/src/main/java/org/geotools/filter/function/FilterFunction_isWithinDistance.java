/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.filter.function;

// this code is autogenerated - you shouldnt be modifying it!

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.locationtech.jts.geom.Geometry;

public class FilterFunction_isWithinDistance extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl(
            "isWithinDistance",
            Boolean.class,
            parameter("geometry1", Geometry.class),
            parameter("geometry2", Geometry.class),
            parameter("distance", Double.class));

    public FilterFunction_isWithinDistance() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        Geometry arg0;
        Geometry arg1;
        double arg2;

        try { // attempt to get value and perform conversion
            arg0 = getExpression(0).evaluate(feature, Geometry.class);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function isWithinDistance argument #0 - expected type Geometry");
        }

        try { // attempt to get value and perform conversion
            arg1 = getExpression(1).evaluate(feature, Geometry.class);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function isWithinDistance argument #1 - expected type Geometry");
        }

        try { // attempt to get value and perform conversion
            arg2 = getExpression(2).evaluate(feature, Double.class).doubleValue();
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function isWithinDistance argument #2 - expected type double");
        }

        return Boolean.valueOf(StaticGeometry.isWithinDistance(arg0, arg1, arg2));
    }
}
