/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.mbstyle.function;

import java.util.List;
import org.geotools.api.data.Parameter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.text.Text;

/**
 * Function that takes an input and applies a text transformation (one of "lowercase", "uppercase", or "none"). The
 * default transformation is "none". <br>
 * <br>
 * ECQL Examples:
 *
 * <pre>
 *  StringTransform('SoMeString', 'uppercase') evaluates to 'SOMESTRING'
 *  StringTransform('SoMeString', 'lowercase') evaluates to 'somestring'
 *  StringTransform('SoMeString', 'none')      evaluates to 'SoMeString'
 * </pre>
 */
public class StringTransformFunction extends FunctionImpl {

    public static final FunctionName NAME;

    private static final FilterFactory ff2 = CommonFactoryFinder.getFilterFactory(null);

    static {
        Parameter<Object> result = new Parameter<>("result", Object.class, 1, 1);
        Parameter<Object> input = new Parameter<>("input", Object.class, 1, 1);
        Parameter<Object> transform = new Parameter<>(
                "transform",
                Object.class,
                Text.text("transform"),
                Text.text("The transform to perform ('uppercase', 'lowercase', or 'none')"),
                true,
                0,
                1,
                "uppercase",
                null);
        NAME = new FunctionNameImpl("StringTransform", result, input, transform);
    }

    public StringTransformFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object object) {
        return evaluate(object, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Object object, Class<T> context) {
        List<Expression> parameters = getParameters();

        Expression input = parameters.get(0);

        Expression transform = parameters.get(1);
        String transformEvaluated = transform.evaluate(object, String.class);
        transformEvaluated =
                transformEvaluated == null ? "" : transformEvaluated.trim().toLowerCase();

        Expression output;
        if ("uppercase".equals(transformEvaluated)) {
            output = ff2.function("strToUpperCase", input);
        } else if ("lowercase".equals(transformEvaluated)) {
            output = ff2.function("strToLowerCase", input);
        } else {
            output = input;
        }

        return context == null ? (T) output.evaluate(object) : output.evaluate(object, context);
    }
}
