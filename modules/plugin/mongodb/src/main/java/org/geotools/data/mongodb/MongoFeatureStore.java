/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2014-2015, Boundless
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
package org.geotools.data.mongodb;

import com.mongodb.DBCollection;
import java.io.IOException;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class MongoFeatureStore extends ContentFeatureStore {

    MongoFeatureSource delegate;

    public MongoFeatureStore(ContentEntry entry, Query query, DBCollection collection) {
        super(entry, query);
        delegate = new MongoFeatureSource(entry, query, collection);
    }

    @Override
    public MongoDataStore getDataStore() {
        return (MongoDataStore) super.getDataStore();
    }

    public CollectionMapper getMapper() {
        return delegate.getMapper();
    }

    public void setMapper(CollectionMapper mapper) {
        delegate.setMapper(mapper);
    }

    @Override
    protected boolean handleVisitor(Query query, FeatureVisitor visitor) throws IOException {
        return delegate.handleVisitor(query, visitor);
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        return delegate.buildFeatureType();
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        return delegate.getCountInternal(query);
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        return delegate.getBoundsInternal(query);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
        return delegate.getReaderInternal(query);
    }

    @Override
    protected boolean canOffset(Query query) {
        return delegate.canOffset(query);
    }

    @Override
    protected boolean canLimit(Query query) {
        return delegate.canLimit(query);
    }

    @Override
    protected boolean canRetype(Query query) {
        return delegate.canRetype(query);
    }

    @Override
    protected boolean canSort(Query query) {
        return delegate.canSort(query);
    }

    @Override
    protected boolean canFilter(Query query) {
        return delegate.canFilter(query);
    }

    @Override
    protected FeatureWriter<SimpleFeatureType, SimpleFeature> getWriterInternal(Query query, int flags)
            throws IOException {
        if ((flags & (WRITER_ADD | WRITER_UPDATE | WRITER_COMMIT)) != 0) {
            return new MongoFeatureWriter(delegate.getCollection(), getSchema(), this);
        }
        return null;
    }

    @Override
    protected QueryCapabilities buildQueryCapabilities() {
        return this.delegate.buildQueryCapabilities();
    }
}
