/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014-2015, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.data.wfs.internal.parsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.geotools.data.wfs.internal.ListStoredQueriesRequest;
import org.geotools.data.wfs.internal.ListStoredQueriesResponse;
import org.geotools.data.wfs.internal.WFSOperationType;
import org.geotools.data.wfs.internal.WFSRequest;
import org.geotools.data.wfs.internal.WFSResponse;
import org.geotools.data.wfs.internal.WFSResponseFactory;
import org.geotools.http.HTTPResponse;
import org.geotools.ows.ServiceException;

public class ListStoredQueriesResponseFactory implements WFSResponseFactory {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean canProcess(WFSRequest originatingRequest, String contentType) {
        return originatingRequest instanceof ListStoredQueriesRequest
                && (contentType == null || contentType.startsWith("text/xml"));
    }

    @Override
    public boolean canProcess(WFSOperationType operation) {
        return WFSOperationType.LIST_STORED_QUERIES.equals(operation);
    }

    @Override
    public List<String> getSupportedOutputFormats() {
        return Arrays.asList("text/xml");
    }

    @Override
    public WFSResponse createResponse(WFSRequest request, HTTPResponse response) throws IOException {
        try {
            return new ListStoredQueriesResponse(request, response);
        } catch (ServiceException e) {
            throw new IOException(e);
        }
    }
}
