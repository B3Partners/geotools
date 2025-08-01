/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2025, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.singlestore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PreparedStatementSQLDialect;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBWriter;

/**
 * SingleStore database dialect based on prepared statements.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class SingleStoreDialectPrepared extends PreparedStatementSQLDialect {

    SingleStoreDialect delegate;

    public SingleStoreDialectPrepared(JDBCDataStore dataStore) {
        super(dataStore);
        delegate = new SingleStoreDialect(dataStore);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName, Connection cx) throws SQLException {
        return delegate.includeTable(schemaName, tableName, cx);
    }

    @Override
    public String getNameEscape() {
        return delegate.getNameEscape();
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return delegate.getGeometryTypeName(type);
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getGeometrySRID(schemaName, tableName, columnName, cx);
    }

    @Override
    public void encodeColumnName(String prefix, String raw, StringBuffer sql) {
        delegate.encodeColumnName(prefix, raw, sql);
    }

    @Override
    public void encodeColumnType(String sqlTypeName, StringBuffer sql) {
        delegate.encodeColumnType(sqlTypeName, sql);
    }

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, String prefix, int srid, Hints hints, StringBuffer sql) {
        delegate.encodeGeometryColumn(gatt, prefix, srid, hints, sql);
    }

    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        delegate.registerClassToSqlMappings(mappings);
    }

    @Override
    public void registerSqlTypeToClassMappings(Map<Integer, Class<?>> mappings) {
        delegate.registerSqlTypeToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        delegate.registerSqlTypeNameToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(Map<Integer, String> overrides) {
        delegate.registerSqlTypeToSqlTypeNameOverrides(overrides);
    }

    @Override
    public void encodePostCreateTable(String tableName, StringBuffer sql) {
        delegate.encodePostCreateTable(tableName, sql);
    }

    @Override
    public void encodePostColumnCreateTable(AttributeDescriptor att, StringBuffer sql) {
        delegate.encodePostColumnCreateTable(att, sql);
    }

    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException, IOException {
        delegate.postCreateTable(schemaName, featureType, cx);
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        delegate.encodePrimaryKey(column, sql);
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return delegate.lookupGeneratedValuesPostInsert();
    }

    @Override
    public Object getNextAutoGeneratedValue(String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getNextAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public Object getLastAutoGeneratedValue(String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getLastAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        delegate.encodeGeometryEnvelope(tableName, geometryColumn, sql);
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx) throws SQLException, IOException {
        return delegate.decodeGeometryEnvelope(rs, column, cx);
    }

    @Override
    public Geometry decodeGeometryValue(
            GeometryDescriptor descriptor,
            ResultSet rs,
            String column,
            GeometryFactory factory,
            Connection cx,
            Hints hints)
            throws IOException, SQLException {
        return delegate.decodeGeometryValue(descriptor, rs, column, factory, cx, hints);
    }

    //
    // prepared statement api
    //
    @Override
    public void prepareGeometryValue(
            Class<? extends Geometry> gClass, int dimension, int srid, Class binding, StringBuffer sql) {
        if (gClass != null) {
            sql.append("GeomFromWKB(?)");
        } else {
            super.prepareGeometryValue(gClass, dimension, srid, binding, sql);
        }
    }

    @Override
    public void setGeometryValue(Geometry g, int dimension, int srid, Class binding, PreparedStatement ps, int column)
            throws SQLException {
        if (g != null) {
            ps.setBytes(column, new WKBWriter(dimension).write(g));
        } else {
            ps.setNull(column, Types.OTHER);
        }
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return delegate.isLimitOffsetSupported();
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        delegate.applyLimitOffset(sql, limit, offset);
    }

    @Override
    public void dropIndex(Connection cx, SimpleFeatureType schema, String databaseSchema, String indexName)
            throws SQLException {
        delegate.dropIndex(cx, schema, databaseSchema, indexName);
    }

    @Override
    public boolean canGroupOnGeometry() {
        return delegate.canGroupOnGeometry();
    }

    @Override
    public Filter[] splitFilter(Filter filter, SimpleFeatureType schema) {
        return delegate.splitFilter(filter, schema);
    }
}
