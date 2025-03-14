/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.filter.text.ecql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.referencing.FactoryException;
import org.geotools.filter.text.commons.CompilerUtil;
import org.geotools.filter.text.commons.Language;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.cql2.CQLGeoOperationTest;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * ECQL Geo Operation Test
 *
 * <p>Execute all cql test for spatial relation using the ECQL compiler
 *
 * <p>
 *
 * <pre>
 *   &lt;routine invocation &gt; ::=
 *           &lt;geoop name &gt; &lt;georoutine argument list &gt;
 *       |   &lt;relgeoop name &gt; &lt;relgeoop argument list &gt;
 *       |   &lt;routine name &gt; &lt;argument list &gt;
 *       |   &lt;relate geoop &gt;
 *
 *   &lt;geoop name &gt; ::=
 *           EQUALS | DISJOINT | INTERSECTS | TOUCHES | CROSSES |
 *           WITHIN | CONTAINS |OVERLAPS
 *
 *   &lt;relate geoop &gt; ::=  &lt;RELATE>&gt; &quot;(&quot; Attribute()&quot;,&quot; &lt;geometry literal&gt; &quot;,&quot; &lt;DE9IM_PATTERN&gt; &quot;)&quot;
 *   &lt;DE9IM_PATTERN&gt; ::= &lt;DIMENSION_CHAR&gt;&lt;DIMENSION_CHAR&gt;&lt;DIMENSION_CHAR&gt; ...
 *   &lt;DIMENSION_CHAR&gt; ::= *| T | F | 0 | 1 | 2
 *
 *   That rule is extended with bbox for convenience.
 *   &lt;bbox argument list &gt;::=
 *       &quot;(&quot;  &lt;attribute &gt; &quot;,&quot; &lt;min X &gt; &quot;,&quot; &lt;min Y &gt; &quot;,&quot; &lt;max X &gt; &quot;,&quot; &lt;max Y &gt;[&quot;,&quot;  &lt;srs &gt;] &quot;)&quot;
 *       &lt;min X &gt; ::=  &lt;signed numerical literal &gt;
 *       &lt;min Y &gt; ::=  &lt;signed numerical literal &gt;
 *       &lt;max X &gt; ::=  &lt;signed numerical literal &gt;
 *       &lt;max Y &gt; ::=  &lt;signed numerical literal &gt;
 *       &lt;srs &gt; ::=
 *
 * </pre>
 *
 * <p>Note: the symbols of geoop names were changed to adjust these name to geoapi filter.
 *
 * @author Mauricio Pazos (Axios Engineering)
 * @since 2.6
 */
public final class ECQLGeoOperationTest extends CQLGeoOperationTest {

    public ECQLGeoOperationTest() {
        super(Language.ECQL);
    }

    @Override
    @Test
    public void disjoint() throws CQLException {

        Filter resultFilter = CompilerUtil.parseFilter(language, "DISJOINT(the_geom, POINT(1 2))");

        assertTrue("Disjoint was expected", resultFilter instanceof Disjoint);
    }

    @Override
    @Test
    public void Intersects() throws CQLException {

        Filter resultFilter = CompilerUtil.parseFilter(language, "INTERSECTS(the_geom, POINT(1 2))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
    }

    @Test
    public void IntersectsWithExpressions() throws CQLException {

        Filter resultFilter =
                CompilerUtil.parseFilter(language, "INTERSECTS(POLYGON((1 2, 2 2, 2 3, 1 2)), POINT(1 2))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
    }

    @Override
    @Test
    public void touches() throws CQLException {
        Filter resultFilter = CompilerUtil.parseFilter(language, "TOUCHES(the_geom, POINT(1 2))");

        assertTrue("Touches was expected", resultFilter instanceof Touches);
    }

    @Override
    @Test
    public void crosses() throws CQLException {
        Filter resultFilter = CompilerUtil.parseFilter(language, "CROSSES(the_geom, POINT(1 2))");

        assertTrue("Crosses was expected", resultFilter instanceof Crosses);
    }

    @Override
    @Test
    public void contains() throws CQLException {
        Filter resultFilter = CompilerUtil.parseFilter(language, "CONTAINS(the_geom, POINT(1 2))");

        assertTrue("Contains was expected", resultFilter instanceof Contains);
    }

    @Override
    @Test
    public void overlaps() throws Exception {

        Filter resultFilter = CompilerUtil.parseFilter(language, "OVERLAPS(the_geom, POINT(1 2))");

        assertTrue("Overlaps was expected", resultFilter instanceof Overlaps);
    }

    @Override
    @Test
    public void equals() throws CQLException {
        // EQUALS
        Filter resultFilter = CompilerUtil.parseFilter(language, "EQUALS(the_geom, POINT(1 2))");

        assertTrue("not an instance of Equals", resultFilter instanceof Equals);
    }

    @Test
    public void functionAsFirstArgument() throws CQLException {

        Filter resultFilter = CompilerUtil.parseFilter(language, "INTERSECTS(centroid(the_geom), POINT(1 2))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
    }

    @Test
    public void functionAsSecondArgument() throws CQLException {

        Filter resultFilter = CompilerUtil.parseFilter(language, "INTERSECTS(the_geom, buffer(POINT(1 2),10))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);

        resultFilter = CompilerUtil.parseFilter(language, "INTERSECTS(the_geom, buffer(the_geom,10))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
    }

    @Test
    public void functionAsFirstAndSecondArgument() throws CQLException {

        Filter resultFilter =
                CompilerUtil.parseFilter(language, "INTERSECTS(centroid(the_geom), buffer(POINT(1 2) ,10))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
    }

    @Test
    public void intersectsWithReferencedGeometry() throws CQLException, FactoryException {

        Filter resultFilter = CompilerUtil.parseFilter(language, "INTERSECTS(the_geom, SRID=4326;POINT(1 2))");

        assertTrue("Intersects was expected", resultFilter instanceof Intersects);
        Intersects intersects = (Intersects) resultFilter;
        Literal geomLiteral = (Literal) intersects.getExpression2();
        assertEquals(
                CRS.decode("EPSG:4326", true),
                geomLiteral.evaluate(null, Geometry.class).getUserData());
    }

    @Test
    public void testIntersectsEnvelope() throws CQLException {
        Filter filter = CompilerUtil.parseFilter(language, "INTERSECTS(geom, ENVELOPE(10, 40, 40, 10))");

        Intersects intersects = (Intersects) filter;
        PropertyName pn = (PropertyName) intersects.getExpression1();
        assertEquals("geom", pn.getPropertyName());

        Polygon polygon = intersects.getExpression2().evaluate(null, Polygon.class);
        assertNotNull(polygon);
        assertTrue(polygon.isRectangle());
        assertEquals(new Envelope(10, 40, 10, 40), polygon.getEnvelopeInternal());
    }
}
