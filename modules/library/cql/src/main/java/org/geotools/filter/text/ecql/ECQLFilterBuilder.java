/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.BinarySpatialOperator;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.text.commons.AbstractFilterBuilder;
import org.geotools.filter.text.commons.BuildResultStack;
import org.geotools.filter.text.commons.IToken;
import org.geotools.filter.text.commons.Result;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Builds the filters required by the {@link ECQLCompiler}.
 *
 * @author Mauricio Pazos (Axios Engineering)
 * @since 2.6
 */
final class ECQLFilterBuilder extends AbstractFilterBuilder {

    public ECQLFilterBuilder(String ecqlSource, FilterFactory filterFactory) {
        super(ecqlSource, filterFactory);
    }

    /**
     * builds the filter id
     *
     * @param token <character>
     * @return String without the quotes
     */
    public FeatureId buildFeatureID(IToken token) {

        String strId = removeQuotes(token.toString());

        FeatureId id = getFilterFactory().featureId(strId);

        return id;
    }

    /**
     * builds the filter id
     *
     * @return Id
     */
    public Id buildFilterId(final int nodeFeatureId) throws CQLException {

        // retrieves the id from stack
        List<FeatureId> idList = new LinkedList<>();
        while (!getResultStack().empty()) {

            Result result = getResultStack().peek();

            int node = result.getNodeType();
            if (node != nodeFeatureId) {
                break;
            }
            FeatureId id = (FeatureId) result.getBuilt();
            idList.add(id);
            getResultStack().popResult();
        }
        assert !idList.isEmpty() : "must have one or more FeatureIds";

        // shorts the id list and builds the filter Id
        Collections.reverse(idList);
        Set<FeatureId> idSet = new LinkedHashSet<>(idList);
        Id filter = getFilterFactory().id(idSet);

        return filter;
    }

    /**
     * Builds a negative Number
     *
     * @return Negative number
     */
    public Literal bulidNegativeNumber() throws CQLException {

        // retrieves the number value from stack and adds the (-) minus
        Literal literal = getResultStack().popLiteral();
        String strNumber = "-" + literal.getValue();
        Object value = literal.getValue();

        // builds the negative number
        @SuppressWarnings("unused")
        Number number = null;
        if (value instanceof Double) {
            number = Double.parseDouble(strNumber);
        } else if (value instanceof Float) {
            number = Float.parseFloat(strNumber);
        } else if (value instanceof Integer) {
            number = Integer.parseInt(strNumber);
        } else if (value instanceof Long) {
            number = Long.parseLong(strNumber);
        } else {
            assert false : "Number instnce is expected";
        }
        Literal signedNumber = getFilterFactory().literal(number);

        return signedNumber;
    }

    /**
     * builds the or filter for the in predicate. The method retrieves the list of expressions and the property name
     * from stack to make the Or filter.
     *
     * <pre>
     * Thus if the stack have the following predicate
     * propName in (expr1, expr2)
     * this method will produce:
     * (propName = expr1) or (propName = expr2)
     * </pre>
     */
    public Or buildInPredicate(final int nodeExpression) throws CQLException {
        // retrieves the expressions from stack
        List<Expression> exprList = new LinkedList<>();
        while (!getResultStack().empty()) {

            Result result = getResultStack().peek();

            int node = result.getNodeType();
            if (node != nodeExpression) {
                break;
            }
            getResultStack().popResult();

            Expression expr = getResultStack().popExpression();
            exprList.add(expr);
        }

        assert !exprList.isEmpty() : "must have one or more expressions";

        // retrieve the left hand expression from the stack
        final Expression leftHandExpr = getResultStack().popExpression();

        // makes one comparison for each expression in the expression list,
        // associated by the Or filter.
        List<Filter> filterList = new LinkedList<>();
        for (Expression expression : exprList) {
            PropertyIsEqualTo eq = getFilterFactory().equals(leftHandExpr, expression);
            filterList.add(eq);
        }
        Collections.reverse(filterList);
        Or orFilter = getFilterFactory().or(filterList);

        return orFilter;
    }

    public Coordinate buildCoordinate() throws CQLException {

        double y = getResultStack().popDoubleValue();
        double x = getResultStack().popDoubleValue();

        Coordinate coordinate = new Coordinate(x, y);

        return coordinate;
    }

    public Point buildPointText() throws CQLException {

        PointBuilder builder = new PointBuilder(getStatement(), getResultStack());

        Point point = (Point) builder.build();

        return point;
    }

    public LineString buildLineString(final int pointNode) throws CQLException {

        LineStringBuilder builder = new LineStringBuilder(getStatement(), getResultStack());

        LineString line = (LineString) builder.build(pointNode);

        return line;
    }

    public Polygon buildPolygon(final int linestringNode) throws CQLException {

        PolygonBuilder builder = new PolygonBuilder(getStatement(), getResultStack());

        Polygon polygon = (Polygon) builder.build(linestringNode);

        return polygon;
    }

    /**
     * Retrieves all points built in previous parsing process from stack and creates the multipoint geometry.
     *
     * @return a MultiPoint
     */
    public MultiPoint buildMultiPoint(int pointNode) throws CQLException {

        MultiPointBuilder builder = new MultiPointBuilder(getStatement(), getResultStack());

        MultiPoint mp = (MultiPoint) builder.build(pointNode);

        return mp;
    }

    /**
     * Retrieves all linestring built from stack and creates the multilinestring geometry
     *
     * @return a MultiLineString
     * @throws CQLException ¡
     */
    public MultiLineString buildMultiLineString(final int linestringtextNode) throws CQLException {

        MultiLineStringBuilder builder = new MultiLineStringBuilder(getStatement(), getResultStack());

        MultiLineString ml = (MultiLineString) builder.build(linestringtextNode);

        return ml;
    }

    /**
     * Builds a {@link MuliPolygon} using the {@link Polygon} staked in the parsing process
     *
     * @param polygontextNode .
     * @return MultiPolygon
     */
    public MultiPolygon buildMultiPolygon(final int polygontextNode) throws CQLException {

        MultiPolygonBuilder builder = new MultiPolygonBuilder(getStatement(), getResultStack());

        MultiPolygon mp = (MultiPolygon) builder.build(polygontextNode);

        return mp;
    }

    /**
     * Builds a {@link GeometryCollection}
     *
     * @return GeometryCollection
     */
    public GeometryCollection buildGeometryCollection(final int jjtgeometryliteral) throws CQLException {

        GeometryCollectionBuilder builder = new GeometryCollectionBuilder(getStatement(), getResultStack());

        GeometryCollection gc = (GeometryCollection) builder.build(jjtgeometryliteral);

        return gc;
    }

    /**
     * Builds literal geometry
     *
     * @return a Literal Geometry
     */
    public Literal buildGeometry() throws CQLException {

        Geometry geometry = getResultStack().popGeometry();

        Literal literal = getFilterFactory().literal(geometry);

        return literal;
    }

    public Literal buildReferencedGeometryLiteral() throws CQLException {
        BuildResultStack resultStack = getResultStack();
        Literal literal = resultStack.popLiteral();
        int srid = resultStack.popIntegerValue();
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:" + srid);
            Geometry geometry = (Geometry) literal.getValue();
            geometry.setUserData(crs);
        } catch (FactoryException e) {
            throw new CQLException("Failed to build CRS for SRID: " + srid, null, e, getStatement());
        }

        return literal;
    }

    public Literal buildGeometryLiteral() throws CQLException {
        // skip the container node
        Result result = getResultStack().popResult();
        return (Literal) result.getBuilt();
    }

    public Literal buildSimpleGeometryLiteral() throws CQLException {
        return getResultStack().popLiteral();
    }

    @Override
    public BinarySpatialOperator buildSpatialEqualFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());
        BinarySpatialOperator filter = builder.buildEquals();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialDisjointFilter() throws CQLException {
        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildDisjoint();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialIntersectsFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildIntersects();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialTouchesFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildTouches();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialCrossesFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildCrosses();

        return filter;
    }

    /**
     * Makes an equals to true filter with the relatePattern function
     *
     * @return relatePattern is equal to true
     */
    public PropertyIsEqualTo buildRelatePattern() throws CQLException {

        RelatePatternBuilder builder = new RelatePatternBuilder(getResultStack(), getFilterFactory());

        Function relatePattern = builder.build();

        PropertyIsEqualTo eq =
                getFilterFactory().equals(relatePattern, getFilterFactory().literal(true));

        return eq;
    }

    /**
     * Builds a not equal filter with that evaluate the relate pattern function
     *
     * @return Not filter
     */
    public Not buildNotRelatePattern() throws CQLException {

        PropertyIsEqualTo eq = buildRelatePattern();

        Not notFilter = getFilterFactory().not(eq);

        return notFilter;
    }

    /**
     * Checks the correctness of pattern and makes a literal with this pattern;
     *
     * @return a Literal with the pattern
     * @throws CQLException if the pattern has not one of the following characters:T,F,*,0,1,2
     */
    public Literal buildPattern9IM() throws CQLException {

        // retrieves the pattern from stack
        Result resut = getResultStack().popResult();
        IToken token = resut.getToken();

        Literal built = (Literal) resut.getBuilt();
        final String pattern = (String) built.getValue();

        // validates the length
        if (pattern.length() != 9) {
            throw new CQLException("the pattern DE-9IM must have nine (9) characters", token, getStatement());
        }

        // validates that the pattern has only the characters T,F,*,0,1,2
        String patternUC = pattern.toUpperCase();

        char[] validFlags = {'T', 'F', '*', '0', '1', '2'};
        for (int i = 0; i < validFlags.length; i++) {
            char character = patternUC.charAt(i);

            boolean found = false;
            for (char validFlag : validFlags) {
                if (validFlag == character) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new CQLException(
                        "the pattern DE-9IM must have only the following characters: T, F, *, 0, 1, 2",
                        token,
                        getStatement());
            }
        }

        Literal patternExpr = getFilterFactory().literal(pattern);

        return patternExpr;
    }

    @Override
    public BinarySpatialOperator buildSpatialWithinFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildWithin();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialContainsFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildContains();

        return filter;
    }

    @Override
    public BinarySpatialOperator buildSpatialOverlapsFilter() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BinarySpatialOperator filter = builder.buildOverlaps();

        return filter;
    }

    /**
     * An equals filter with to test the relate function
     *
     * @return Relate equals true
     */
    public PropertyIsEqualTo buildRelate() throws CQLException {

        RelateBuilder builder = new RelateBuilder(getResultStack(), getFilterFactory());

        Function f = builder.build();

        PropertyIsEqualTo eq = getFilterFactory().equals(f, getFilterFactory().literal(true));

        return eq;
    }

    @Override
    public org.geotools.api.filter.spatial.BBOX buildBBox() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BBOX filter = builder.buildBBox();

        return filter;
    }

    @Override
    public org.geotools.api.filter.spatial.BBOX buildBBoxWithCRS() throws CQLException {

        SpatialOperationBuilder builder = new SpatialOperationBuilder(getResultStack(), getFilterFactory());

        BBOX filter = builder.buildBBoxWithCRS();

        return filter;
    }

    @Override
    public Literal buildEnvelope(IToken token) throws CQLException {
        Literal result = super.buildEnvelope(token);

        // unlike CQL, in ECQL the numbers are tokens loaded on the stack, they need to be
        // removed
        BuildResultStack stack = getResultStack();
        stack.popResult();
        stack.popResult();
        stack.popResult();
        stack.popResult();

        return result;
    }
}
