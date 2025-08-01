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

public class FilterFunction_if_then_else extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl(
            "if_then_else",
            Object.class,
            parameter("condition", Boolean.class),
            parameter("then", Object.class),
            parameter("else", Object.class));

    public FilterFunction_if_then_else() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        boolean select;
        Object arg1;
        Object arg2;

        try { // attempt to get value and perform conversion
            select = getExpression(0).evaluate(feature, Boolean.class);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function if_then_else argument #0 - expected type boolean");
        }
        if (select) {
            try { // attempt to get value and perform conversion
                arg1 = getExpression(1).evaluate(feature);
                return arg1;
            } catch (Exception e) // probably a type error
            {
                throw new IllegalArgumentException(
                        "Filter Function problem for function if_then_else argument #1 - expected type Object");
            }
        } else {
            try { // attempt to get value and perform conversion
                arg2 = getExpression(2).evaluate(feature);
                return arg2;
            } catch (Exception e) // probably a type error
            {
                throw new IllegalArgumentException(
                        "Filter Function problem for function if_then_else argument #2 - expected type Object");
            }
        }
    }
}
