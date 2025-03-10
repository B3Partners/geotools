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
package org.geotools.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.api.feature.IllegalAttributeException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterTest.MockDataObject;
import org.geotools.filter.expression.AddImpl;
import org.geotools.filter.expression.DivideImpl;
import org.geotools.filter.expression.MultiplyImpl;
import org.geotools.filter.expression.SubtractImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Unit test for expressions. This is a complimentary test suite with the filter test suite.
 *
 * @author James MacGill, CCG
 * @author Rob Hranac, TOPP
 */
public class ExpressionTest {
    /** Standard logging instance */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ExpressionTest.class);

    /** Feature on which to preform tests */
    private static SimpleFeature testFeature = null;

    /** Schema on which to preform tests */
    private static SimpleFeatureType testSchema = null;

    static FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    boolean set = false;

    /**
     * Sets up a schema and a test feature.
     *
     * @throws SchemaException If there is a problem setting up the schema.
     * @throws IllegalFeatureException If problem setting up the feature.
     */
    @Before
    public void setUp() throws SchemaException, IllegalAttributeException {
        if (set) {
            return;
        }

        set = true;

        // Create the schema attributes
        LOGGER.finer("creating flat feature...");

        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setCRS(null);
        ftb.add("testGeometry", LineString.class);
        ftb.add("testBoolean", Boolean.class);
        ftb.add("testCharacter", Character.class);
        ftb.add("testByte", Byte.class);
        ftb.add("testShort", Short.class);
        ftb.add("testInteger", Integer.class);
        ftb.add("testLong", Long.class);
        ftb.add("testFloat", Float.class);
        ftb.add("testDouble", Double.class);
        ftb.add("testString", String.class);
        ftb.add("testZeroDouble", Double.class);
        ftb.add("testList", Collection.class);
        ftb.add("testList2", Collection.class);
        ftb.setName("testSchema");
        testSchema = ftb.buildFeatureType();

        // Creates coordinates for the linestring
        Coordinate[] coords = new Coordinate[3];
        coords[0] = new Coordinate(1, 2);
        coords[1] = new Coordinate(3, 4);
        coords[2] = new Coordinate(5, 6);

        // Builds the test feature
        Object[] attributes = new Object[10];
        GeometryFactory gf = new GeometryFactory(new PrecisionModel());
        attributes[0] = gf.createLineString(coords);
        attributes[1] = Boolean.TRUE;
        attributes[2] = Character.valueOf('t');
        attributes[3] = Byte.valueOf("10");
        attributes[4] = Short.valueOf("101");
        attributes[5] = Integer.valueOf(1002);
        attributes[6] = Long.valueOf(10003);
        attributes[7] = Float.valueOf(10000.4f);
        attributes[8] = Double.valueOf(100000.5);
        attributes[9] = "test string data";

        // Creates the feature itself
        // FlatFeatureFactory factory = new FlatFeatureFactory(testSchema);
        testFeature = SimpleFeatureBuilder.build(testSchema, attributes, null);
        // support for properties with lists
        testFeature.setAttribute("testList", Arrays.asList(1, 2, 3, 4));
        LOGGER.finer("...feature created");
    }

    /**
     * Tests the attribute expression.
     *
     * @throws IllegalFilterException if filters mess up.
     */
    @Test
    public void testAttribute() throws IllegalFilterException {
        // Test integer attribute
        Expression testAttribute = new AttributeExpressionImpl(testSchema, "testInteger");
        LOGGER.fine("integer attribute expression equals: " + testAttribute.evaluate(testFeature));
        Assert.assertEquals(Integer.valueOf(1002), testAttribute.evaluate(testFeature));

        // Test string attribute
        testAttribute = new AttributeExpressionImpl(testSchema, "testString");
        LOGGER.fine("string attribute expression equals: " + testAttribute.evaluate(testFeature));
        Assert.assertEquals("test string data", testAttribute.evaluate(testFeature));
    }

    /**
     * Tests the attribute expression over an object other than Feature.
     *
     * @throws IllegalFilterException if filters mess up.
     */
    @Test
    public void testAttributeObject() throws IllegalFilterException {
        MockDataObject testFeature = new MockDataObject(10, "diez");

        // Test integer attribute
        org.geotools.api.filter.expression.Expression testAttribute = new AttributeExpressionImpl("intVal");

        Assert.assertEquals(Integer.valueOf(10), testAttribute.evaluate(testFeature));

        // Test string attribute
        testAttribute = new AttributeExpressionImpl("stringVal");

        Assert.assertEquals("diez", testAttribute.evaluate(testFeature));
    }

    /**
     * Tests the literal expression.
     *
     * @throws IllegalFilterException if there are problems
     */
    @Test
    public void testLiteral() throws IllegalFilterException {
        // Test integer attribute
        Expression testLiteral = new LiteralExpressionImpl(Integer.valueOf(1002));
        LOGGER.fine("integer literal expression equals: " + testLiteral.evaluate(testFeature));
        Assert.assertEquals(Integer.valueOf(1002), testLiteral.evaluate(testFeature));

        // Test string attribute
        testLiteral = new LiteralExpressionImpl("test string data");
        LOGGER.fine("string literal expression equals: " + testLiteral.evaluate(testFeature));
        Assert.assertEquals("test string data", testLiteral.evaluate(testFeature));
    }

    /**
     * Tests the literal expression over an object other than Feature.
     *
     * @throws IllegalFilterException if there are problems
     */
    @Test
    public void testLiteralObject() throws IllegalFilterException {
        MockDataObject testObj = new MockDataObject(1000, "mil");

        // Test integer attribute
        org.geotools.api.filter.expression.Expression testLiteral = new LiteralExpressionImpl(Integer.valueOf(1002));

        Assert.assertEquals(Integer.valueOf(1002), testLiteral.evaluate(testObj));

        // Test string attribute
        testLiteral = new LiteralExpressionImpl("test string data");

        Assert.assertEquals("test string data", testLiteral.evaluate(testObj));
    }

    @Test
    public void testMinFunction() {
        PropertyName a = ff.property("testInteger");
        Literal b = ff.literal(1004.0);
        Function min = ff.function("min", a, b);
        Assert.assertNotNull(min);
    }

    /**
     * Tests the min function expression.
     *
     * @throws IllegalFilterException if filter problems
     */
    @Test
    public void testMinFunctionOld() throws IllegalFilterException {
        Expression a = new AttributeExpressionImpl(testSchema, "testInteger");
        Expression b = new LiteralExpressionImpl(Double.valueOf(1004));

        Function min = ff.function("min", a, b);

        Object value = min.evaluate(testFeature);
        Assert.assertEquals(1002d, ((Double) value).doubleValue(), 0);

        b = ff.literal(Double.valueOf(-100.001));
        min = ff.function("min", a, b);

        value = min.evaluate(testFeature);
        Assert.assertEquals(-100.001, ((Double) value).doubleValue(), 0);
    }

    @Test
    public void testNonExistentFunction() {
        try {
            Function nochance = ff.function("%$#%$%#%#$@#%@", (org.geotools.api.filter.expression.Expression) null);
            Assert.assertNull(nochance);
        } catch (RuntimeException re) {
        }
    }

    @Test
    public void testFunctionNameTrim() throws IllegalFilterException {
        Function min = ff.function("minFunction", ff.literal(2), ff.literal(3));
        Assert.assertNotNull(min);
    }

    /**
     * Tests the max function expression.
     *
     * @throws IllegalFilterException if filter problems
     */
    @Test
    public void testMaxFunction() throws IllegalFilterException {
        Expression a = new AttributeExpressionImpl(testSchema, "testInteger");
        Expression b = new LiteralExpressionImpl(Double.valueOf(1004));

        Function max = ff.function("max", a, b);
        Assert.assertEquals(1004d, ((Double) max.evaluate(testFeature)).doubleValue(), 0);

        b = new LiteralExpressionImpl(Double.valueOf(-100.001));
        max = ff.function("max", a, b);
        Assert.assertEquals(1002d, ((Double) max.evaluate(testFeature)).doubleValue(), 0);
    }

    /**
     * Tests the max function expression over other kind of object than Feature.
     *
     * @throws IllegalFilterException if filter problems
     */
    @Test
    public void testMaxFunctionObject() throws IllegalFilterException {
        MockDataObject testObj = new MockDataObject(10, "diez");
        org.geotools.api.filter.expression.Expression a = new AttributeExpressionImpl("intVal");
        org.geotools.api.filter.expression.Expression b = new LiteralExpressionImpl(Double.valueOf(1004));

        Function max = ff.function("max", a, b);
        Assert.assertEquals("max", max.getName());

        Object maxValue = max.evaluate(testObj);
        Assert.assertEquals(1004d, ((Double) maxValue).doubleValue(), 0);

        b = new LiteralExpressionImpl(Double.valueOf(-100.001));

        max = ff.function("max", a, b);
        maxValue = max.evaluate(testObj);

        Assert.assertEquals(10, ((Double) maxValue).doubleValue(), 0);
    }

    @Test
    public void testIncompleteMathExpression() throws IllegalFilterException {
        Expression testAttribute1 = new LiteralExpressionImpl(Integer.valueOf(4));

        MathExpressionImpl mathTest = new AddImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        try {
            mathTest.evaluate(testFeature);
            Assert.fail("math expressions should not work if right hand side is not set");
        } catch (IllegalArgumentException ife) {
        }
        mathTest = new AddImpl(null, null);
        mathTest.setExpression2(testAttribute1);
        try {
            mathTest.evaluate(testFeature);
            Assert.fail("math expressions should not work if left hand side is not set");
        } catch (IllegalArgumentException ife) {
        }
    }

    /**
     * Tests the math expression.
     *
     * @throws IllegalFilterException if filter problems
     */
    @Test
    public void testMath() throws IllegalFilterException {
        // Test integer attribute
        Expression testAttribute1 = new LiteralExpressionImpl(Integer.valueOf(4));
        Expression testAttribute2 = new LiteralExpressionImpl(Integer.valueOf(2));

        // Test addition
        MathExpressionImpl mathTest = new AddImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);
        LOGGER.fine("math test: "
                + testAttribute1.evaluate(testFeature)
                + " + "
                + testAttribute2.evaluate(testFeature)
                + " = "
                + mathTest.evaluate(testFeature));
        Assert.assertEquals(Integer.valueOf(6), mathTest.evaluate(testFeature, Integer.class));

        // Test subtraction
        mathTest = new SubtractImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);
        LOGGER.fine("math test: "
                + testAttribute1.evaluate(testFeature)
                + " - "
                + testAttribute2.evaluate(testFeature)
                + " = "
                + mathTest.evaluate(testFeature));
        Assert.assertEquals(Integer.valueOf(2), mathTest.evaluate(testFeature, Integer.class));

        // Test multiplication
        mathTest = new MultiplyImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);
        LOGGER.fine("math test: "
                + testAttribute1.evaluate(testFeature)
                + " * "
                + testAttribute2.evaluate(testFeature)
                + " = "
                + mathTest.evaluate(testFeature));
        Assert.assertEquals(Integer.valueOf(8), mathTest.evaluate(testFeature, Integer.class));

        // Test division
        mathTest = new DivideImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);
        LOGGER.fine("math test: "
                + testAttribute1.evaluate(testFeature)
                + " / "
                + testAttribute2.evaluate(testFeature)
                + " = "
                + mathTest.evaluate(testFeature));
        Assert.assertEquals(Double.valueOf(2), mathTest.evaluate(testFeature));
    }

    /**
     * Tests the math expression over other kind of object than Feature.
     *
     * @throws IllegalFilterException if filter problems
     */
    @Test
    public void testMathObject() throws IllegalFilterException {
        MockDataObject testObject = new MockDataObject(10, "diez");

        // Test integer attribute
        org.geotools.api.filter.expression.Expression testAttribute1 = new LiteralExpressionImpl(Integer.valueOf(4));
        org.geotools.api.filter.expression.Expression testAttribute2 = new LiteralExpressionImpl(Integer.valueOf(2));

        // Test addition
        MathExpressionImpl mathTest = new AddImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);

        Assert.assertEquals(Integer.valueOf(6), mathTest.evaluate(testObject, Integer.class));

        // Test subtraction
        mathTest = new SubtractImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);

        Assert.assertEquals(Integer.valueOf(2), mathTest.evaluate(testObject, Integer.class));

        // Test multiplication
        mathTest = new MultiplyImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);

        Assert.assertEquals(Integer.valueOf(8), mathTest.evaluate(testObject, Integer.class));

        // Test division
        mathTest = new DivideImpl(null, null);
        mathTest.setExpression1(testAttribute1);
        mathTest.setExpression2(testAttribute2);

        Assert.assertEquals(Double.valueOf(2), mathTest.evaluate(testObject));
    }

    @Test
    public void testMathObjectwithLists() throws IllegalFilterException {
        FilterFactory ff = new FilterFactoryImpl();
        // Multiply Test
        // list x 2
        MathExpressionImpl mathExpression = new MultiplyImpl(ff.property("testList"), ff.literal(Integer.valueOf(2)));
        List scaledList = (List) mathExpression.evaluate(testFeature);
        // verify multiplication
        Assert.assertEquals(Double.valueOf(2), scaledList.get(0));

        // list - 1
        mathExpression = new SubtractImpl(ff.property("testList"), ff.literal(Integer.valueOf(1)));
        List subtractedList = (List) mathExpression.evaluate(testFeature);
        Assert.assertEquals(Double.valueOf(0), subtractedList.get(0));

        // list + 1
        mathExpression = new AddImpl(ff.literal(Integer.valueOf(1)), ff.property("testList"));
        List addedList = (List) mathExpression.evaluate(testFeature);
        Assert.assertEquals(Double.valueOf(2), addedList.get(0));

        // list / 2
        mathExpression = new DivideImpl(ff.literal(Integer.valueOf(2)), ff.property("testList"));
        List dividedList = (List) mathExpression.evaluate(testFeature);
        Assert.assertEquals(Double.valueOf(0.5), dividedList.get(0));
    }
}
