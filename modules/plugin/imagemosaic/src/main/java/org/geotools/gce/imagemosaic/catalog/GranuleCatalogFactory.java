/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007 - 2016, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic.catalog;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.Repository;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.Converters;
import org.geotools.util.URLs;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Simple Factory class for creating {@link GranuleCatalog} instance to handle the catalog of granules for this mosaic.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public abstract class GranuleCatalogFactory {

    private static final Logger LOGGER = Logging.getLogger(GranuleCatalogFactory.class);

    /** Default private constructor to enforce singleton */
    private GranuleCatalogFactory() {}

    public static GranuleCatalog createGranuleCatalog(
            final Properties params,
            final CatalogConfigurationBeans configurations,
            final boolean caching,
            final boolean create,
            final DataStoreFactorySpi spi,
            final Hints hints) {
        // build the catalog
        Repository repository = (Repository) hints.get(Hints.REPOSITORY);
        String storeName = (String) params.get(Utils.Prop.STORE_NAME);
        AbstractGTDataStoreGranuleCatalog gtCatalog;
        if (storeName != null && !storeName.trim().isEmpty()) {
            if (repository == null) {
                throw new IllegalArgumentException(
                        "Was given a store name " + storeName + " but there is no Repository to resolve it");
            } else {
                gtCatalog = new RepositoryDataStoreCatalog(
                        params, configurations, create, repository, storeName, spi, hints);
            }
        } else {
            gtCatalog = new GTDataStoreGranuleCatalog(params, configurations, create, spi, hints);
        }
        DataStore store = gtCatalog.getTileIndexStore();

        // caching wrappers
        GranuleCatalog catalog;
        if (caching) {
            if (configurations.size() != 1)
                throw new IllegalArgumentException(
                        "Cannot perform in complete memory caching of granules when having multiple coverages");
            catalog = new STRTreeGranuleCatalog(params, gtCatalog, hints);
        } else {
            Integer maxAge = Converters.convert(params.get(Utils.Prop.QUERY_CACHE_MAX_AGE), Integer.class);
            Integer maxFeatures = Converters.convert(params.get(Utils.Prop.QUERY_CACHE_MAX_FEATURES), Integer.class);
            if (maxAge != null && maxFeatures != null) {
                GranuleCatalog queryCache = new QueryCacheGranuleCatalog(gtCatalog, maxFeatures, maxAge);
                catalog = new CachingDataStoreGranuleCatalog(queryCache);
            } else {
                catalog = new CachingDataStoreGranuleCatalog(gtCatalog);
            }
        }

        // locking wrappers
        if (store instanceof Wrapper) {
            try {
                store = Optional.ofNullable((DataStore) ((Wrapper) store).unwrap(JDBCDataStore.class))
                        .orElse(store);
            } catch (IllegalArgumentException e) {
                LOGGER.log(
                        Level.FINER,
                        "The store is a wrapper but does not wrap a JDBCDataStore "
                                + "(not a problem per se, just a note)",
                        e);
            }
        }
        if (!(store instanceof JDBCDataStore)) {
            catalog = new LockingGranuleCatalog(catalog, hints);
        }

        return catalog;
    }

    public static GranuleCatalog createGranuleCatalog(
            final URL sourceURL,
            final CatalogConfigurationBeans configurations,
            final Properties overrideParams,
            final Hints hints) {
        final File sourceFile = URLs.urlToFile(sourceURL);
        final String extension = FilenameUtils.getExtension(sourceFile.getAbsolutePath());

        // STANDARD PARAMS
        final Properties params = new Properties();

        if (sourceURL != null) {
            File parentDirectory = URLs.urlToFile(sourceURL);
            if (parentDirectory.isFile()) parentDirectory = parentDirectory.getParentFile();
            params.put(
                    Utils.Prop.PARENT_LOCATION, URLs.fileToUrl(parentDirectory).toString());
        }
        // SPI
        DataStoreFactorySpi spi = null;

        // Now format specific code
        if (extension.equalsIgnoreCase("shp")) {
            //
            // SHAPEFILE
            //
            params.put(ShapefileDataStoreFactory.URLP.key, sourceURL);
            params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
            params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.TRUE);
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.FALSE);
            params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, Boolean.FALSE);
            params.put(ShapefileDataStoreFactory.DBFTIMEZONE.key, TimeZone.getTimeZone("UTC"));
            spi = Utils.SHAPE_SPI;
        } else {
            // read the properties file
            Properties properties = CoverageUtilities.loadPropertiesFromURL(sourceURL);
            if (properties == null) return null;

            // get the params
            for (Object p : properties.keySet()) {
                params.put(p.toString(), properties.get(p).toString());
            }

            // SPI for datastore
            final String SPIClass = properties.getProperty("SPI");
            try {
                // create a datastore as instructed
                spi = Class.forName(SPIClass)
                        .asSubclass(DataStoreFactorySpi.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                // if we are directed to use a pre-existing store then don't complain about lack of
                // SPI
                if (properties.get(Utils.Prop.STORE_NAME) == null) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                    }
                    return null;
                }
            }
        }
        // Instantiate
        if (overrideParams != null && !overrideParams.isEmpty()) {
            params.putAll(overrideParams);
        }
        return createGranuleCatalog(
                params, configurations, configurations.first().isCaching(), false, spi, hints);
    }
}
