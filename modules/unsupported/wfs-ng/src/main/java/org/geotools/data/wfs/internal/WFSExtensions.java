/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2014, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.wfs.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.util.factory.FactoryNotFoundException;

/**
 * Utility class to look up for a parser that can deal with a given WFS response and process it.
 *
 * <p>This class uses the usual GeoTools SPI (Service Provider Interface) mechanism to find out a
 * {@link WFSResponseFactory} for a given {@link WFSResponse}. As such, {@link WFSResponseFactory} implementation may
 * live outside this plugin as long as they're declared in it's own {code
 * /META-INF/services/org.geotools.data.wfs.protocol.wfs.WFSResponseParserFactory} text file.
 *
 * @author Gabriel Roldan (OpenGeo)
 * @since 2.6
 */
public class WFSExtensions {
    /** The service registry for this manager. Will be initialized only when first needed. */
    private static volatile Set<WFSResponseFactory> registry;

    /**
     * Finds the most suitable WFS response factory for the given request and content-type.
     *
     * @param originatingRequest the WFS request that needs a response factory
     * @param contentType content-type that came in the header of the response
     * @throws FactoryNotFoundException when no factory is found
     */
    public static WFSResponseFactory findResponseFactory(
            final WFSRequest originatingRequest, final String contentType) {

        return getServiceProviders()
                .filter(factory -> factory.isAvailable() && factory.canProcess(originatingRequest, contentType))
                .findFirst()
                .orElseThrow(() -> new FactoryNotFoundException("Can't find a response parser factory for "
                        + originatingRequest.getOperation()
                        + "/'"
                        + contentType
                        + "'"));
    }

    /**
     * Finds all suitable WFS response factories for the operation.
     *
     * @param operation WFS operation that we should have response for
     * @return list of known factories that supports the operation
     */
    public static List<WFSResponseFactory> findResponseFactories(final WFSOperationType operation) {
        return getServiceProviders()
                .filter(factory -> factory.isAvailable() && factory.canProcess(operation))
                .collect(Collectors.toList());
    }

    private static Stream<WFSResponseFactory> getServiceProviders() {
        if (registry == null) {
            synchronized (WFSExtensions.class) {
                if (registry == null) {
                    /*
                     * Set the current thread's class loader to the one that actually loaded the
                     * WDSDataStore and related classes for while the factory lookup is performed.
                     * This way the module is friendlier to crazy class loader hierarchies like
                     * OSGI/Eclipse
                     */
                    final ClassLoader current = Thread.currentThread().getContextClassLoader();
                    try {
                        final ClassLoader tempClassLoader = WFSDataStoreFactory.class.getClassLoader();
                        Thread.currentThread().setContextClassLoader(tempClassLoader);
                        /*
                         * Now that we're on the correct classloader lets perform the lookup
                         */

                        Iterator<WFSResponseFactory> providers =
                                ServiceLoader.load(WFSResponseFactory.class).iterator();
                        Set<WFSResponseFactory> tmp = new HashSet<>();
                        while (providers.hasNext()) {
                            WFSResponseFactory provider = providers.next();
                            tmp.add(provider);
                        }
                        registry = tmp;
                    } finally {
                        /*
                         * And finally restore the original thread's class loader
                         */
                        Thread.currentThread().setContextClassLoader(current);
                    }
                }
            }
        }
        return registry.stream();
    }
}
