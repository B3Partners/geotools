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
package org.geotools.ows.wms;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.ows.AbstractOpenWebService;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Specification;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.request.DescribeLayerRequest;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.request.GetLegendGraphicRequest;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.ows.wms.request.GetStylesRequest;
import org.geotools.ows.wms.request.PutStylesRequest;
import org.geotools.ows.wms.response.DescribeLayerResponse;
import org.geotools.ows.wms.response.GetFeatureInfoResponse;
import org.geotools.ows.wms.response.GetLegendGraphicResponse;
import org.geotools.ows.wms.response.GetMapResponse;
import org.geotools.ows.wms.response.GetStylesResponse;
import org.geotools.ows.wms.response.PutStylesResponse;
import org.geotools.ows.wms.xml.WMSSchema;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.XMLHandlerHints;

/**
 * WebMapServer is a class representing a WMS. It is used to access the Capabilities document and perform requests. It
 * supports multiple versions and will perform version negotiation automatically and use the highest known version that
 * the server can communicate.
 *
 * <p>If restriction of versions to be used is desired, this class should be subclassed and it's setupSpecifications()
 * method over-ridden. It should add which version/specifications are to be used to the specs array. See the current
 * implementation for an example.
 *
 * <p>Example usage: <code><pre>
 * WebMapServer wms = new WebMapServer("http://some.example.com/wms");
 * WMSCapabilities capabilities = wms.getCapabilities();
 * GetMapRequest request = wms.getMapRequest();
 *
 * ... //configure request
 *
 * GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
 *
 * ... //extract image from the response
 * </pre></code>
 *
 * @author Richard Gould, Refractions Research
 */
public class WebMapServer extends AbstractOpenWebService<WMSCapabilities, Layer> {

    /**
     * Class quickly describing Web Map Service.
     *
     * @author Jody
     */
    protected class WMSInfo implements ServiceInfo {

        private Set<String> keywords;
        private Icon icon;

        WMSInfo() {
            keywords = new HashSet<>();
            if (capabilities.getService() != null) {
                String[] array = capabilities.getService().getKeywordList();
                if (array != null) {
                    keywords.addAll(Arrays.asList(array));
                }
            }
            keywords.add("WMS");
            keywords.add(serverURL.toString());

            URL globe2 = WebMapServer.class.getResource("Globe2.png");
            icon = new ImageIcon(globe2);
        }

        @Override
        public String getDescription() {
            String description = null;
            if (capabilities != null && capabilities.getService() != null) {
                description = capabilities.getService().get_abstract();
            }
            if (description == null) {
                description = "Web Map Server " + serverURL;
            }
            return description;
        }

        public Icon getIcon() {
            return icon;
        }

        @Override
        public Set<String> getKeywords() {
            return keywords;
        }

        @Override
        public URI getPublisher() {
            try {
                return capabilities
                        .getService()
                        .getContactInformation()
                        .getContactInfo()
                        .getOnLineResource()
                        .getLinkage();
            } catch (NullPointerException publisherNotAvailable) {
            }
            try {
                return new URI(serverURL.getProtocol() + ":" + serverURL.getHost());
            } catch (URISyntaxException e) {
            }
            return null;
        }

        /**
         * We are a Web Map Service:
         *
         * @return WMSSchema.NAMESPACE;
         */
        @Override
        public URI getSchema() {
            return WMSSchema.NAMESPACE;
        }

        /**
         * The source of this WMS is the capabilities document.
         *
         * <p>We make an effort here to look in the capabilities document provided for the unambiguous capabilities URI.
         * This covers the case where the capabilities document has been cached on disk and we are restoring a
         * WebMapServer instance.
         */
        @Override
        public URI getSource() {
            try {
                URL source = getCapabilities().getRequest().getGetCapabilities().getGet();
                return source.toURI();
            } catch (NullPointerException | URISyntaxException huh) {
            }
            try {
                return serverURL.toURI();
            } catch (URISyntaxException e) {
                return null;
            }
        }

        @Override
        public String getTitle() {
            if (capabilities != null && capabilities.getService() != null) {
                return capabilities.getService().getTitle();
            } else if (serverURL == null) {
                return "Unavailable";
            } else {
                return serverURL.toString();
            }
        }
    }

    /**
     * Quickly describe a layer.
     *
     * @author Jody
     */
    public class LayerInfo implements ResourceInfo {

        private ReferencedEnvelope bounds;
        private Set<String> keywords;
        private Icon icon;
        private Layer layer;

        LayerInfo(Layer layer) {
            this.layer = layer;
            Bounds env = null;
            CoordinateReferenceSystem crs = null;

            if (layer.getBoundingBoxes().isEmpty()) {
                env = layer.getEnvelope(DefaultGeographicCRS.WGS84);
            } else {
                CRSEnvelope bbox;
                String epsg4326 = "EPSG:4326";
                String epsg4269 = "EPSG:4269";
                if (layer.getBoundingBoxes().containsKey(epsg4326)) {
                    bbox = layer.getBoundingBoxes().get(epsg4326);
                } else if (layer.getBoundingBoxes().containsKey(epsg4269)) {
                    bbox = layer.getBoundingBoxes().get(epsg4269);
                } else {
                    bbox = layer.getBoundingBoxes().values().iterator().next();
                }

                try {
                    crs = CRS.decode(bbox.getEPSGCode());
                    env = new ReferencedEnvelope(bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), crs);
                } catch (FactoryException e) {
                    crs = DefaultGeographicCRS.WGS84;
                    env = layer.getEnvelope(crs);
                }
            }
            bounds = new ReferencedEnvelope(
                    env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1), crs);

            String source = getInfo().getSource().toString();

            keywords = new TreeSet<>();

            if (layer.getKeywords() != null) {
                List<String> more = Arrays.asList(layer.getKeywords());
                keywords.addAll(more);
            }
            if (layer.getName() != null) {
                keywords.add(layer.getName());
            }
            if (layer.getTitle() != null) {
                keywords.add(layer.getTitle());
            }
            keywords.add(capabilities.getService().getName());
            keywords.add(source);
            keywords.addAll(getInfo().getKeywords());
        }

        @Override
        public ReferencedEnvelope getBounds() {
            return bounds;
        }

        @Override
        public CoordinateReferenceSystem getCRS() {
            return bounds.getCoordinateReferenceSystem();
        }

        @Override
        public String getDescription() {
            String description = layer.get_abstract();
            if (description != null && description.length() != 0) {
                return description;
            } else {
                return getCapabilities().getService().get_abstract();
            }
        }

        public synchronized Icon getIcon() {
            if (icon == null) {
                URL image = WebMapServer.class.getResource("image.png");
                icon = new ImageIcon(image);
                if (layer.getChildren() != null && layer.getChildren().length != 0) {
                    // Do not request "parent" layer graphics - this kills Mapserver
                    return icon;
                }
                OperationType getLegendGraphic = getCapabilities().getRequest().getGetLegendGraphic();
                if (getLegendGraphic == null) {
                    // this wms server does not support legend graphics
                    return icon;
                }
                GetLegendGraphicRequest request = createGetLegendGraphicRequest();
                request.setLayer(layer.getName());
                request.setWidth("16");
                request.setHeight("16");

                String desiredFormat = null;
                List<String> formats = getLegendGraphic.getFormats();
                if (formats.contains("image/png")) {
                    desiredFormat = "image/png";
                } else if (formats.contains("image/gif")) {
                    desiredFormat = "image/gif";
                } else {
                    // there is no format that I am sure we can deal with :-(
                    // the danger here is requesting PDF from geoserver
                    // (because everyone wants a PDF legend graphics - wot?)
                    return icon;
                }
                request.setFormat(desiredFormat);
                request.setStyle("");

                URL legendGraphics = request.getFinalURL();
                icon = new ImageIcon(legendGraphics);
            }
            return icon;
        }

        @Override
        public Set<String> getKeywords() {
            return keywords;
        }

        @Override
        public String getName() {
            return layer.getName();
        }

        @Override
        public URI getSchema() {
            return getInfo().getSchema();
        }

        @Override
        public String getTitle() {
            String title = layer.getTitle();
            if (title != null && title.length() != 0) {
                return title;
            } else {
                // often the "root" layer has no title, the service title
                // is what is intended
                return getCapabilities().getService().getTitle();
            }
        }
    }

    /**
     * Creates a new WebMapServer from a WMSCapablitiles document.
     *
     * <p>The implementation assumes that the server is located at:
     * capabilities.getRequest().getGetCapabilities().getGet()
     */
    public WebMapServer(WMSCapabilities capabilities) throws IOException, ServiceException {
        super(capabilities.getRequest().getGetCapabilities().getGet(), HTTPClientFinder.createClient(), capabilities);
    }

    /**
     * Creates a new WebMapServer instance and attempts to retrieve the Capabilities document specified by serverURL.
     *
     * @param serverURL a URL that points to the capabilities document of a server
     * @throws IOException if there is an error communicating with the server
     * @throws ServiceException if the server responds with an error
     */
    public WebMapServer(final URL serverURL) throws IOException, ServiceException {
        super(serverURL);
    }

    /**
     * Creates a new WebMapServer instance and attempts to retrieve the Capabilities document specified by serverURL.
     *
     * @param serverURL a URL that points to the capabilities document of a server
     * @throws IOException if there is an error communicating with the server
     * @throws ServiceException if the server responds with an error
     */
    public WebMapServer(final URL serverURL, final HTTPClient httpClient) throws IOException, ServiceException {
        super(serverURL, httpClient);
    }

    /**
     * Creates a new WebMapServer instance and attempts to retrieve the Capabilities document specified by serverURL.
     *
     * @param serverURL a URL that points to the capabilities document of a server
     * @param httpClient The client to be used when performing HTTP requests
     * @param hints A map of hints. Can be used to control some aspects of the XML parsing, see {@link XMLHandlerHints}
     *     for a reference
     * @throws IOException if there is an error communicating with the server
     * @throws ServiceException if the server responds with an error
     */
    public WebMapServer(final URL serverURL, final HTTPClient httpClient, Map<String, Object> hints)
            throws IOException, ServiceException {
        super(serverURL, httpClient, null, hints);
    }

    /**
     * Creates a new WebMapServer instance and retrieve the Capabilities document specified by serverURL.
     *
     * @param serverURL a URL that points to the capabilities document of a server
     * @param httpClient The client to be used when performing HTTP requests
     * @param hints A map of hints. Can be used to control some aspects of the XML parsing, see {@link XMLHandlerHints}
     *     for a reference
     * @param headers A map of headers. These will be added when making the HTTP request
     * @throws IOException if there is an error communicating with the server
     * @throws ServiceException if the server responds with an error
     */
    public WebMapServer(
            final URL serverURL, final HTTPClient httpClient, Map<String, Object> hints, Map<String, String> headers)
            throws IOException, ServiceException {
        super(serverURL, httpClient, null, hints, headers);
    }

    /**
     * Creates a new WebMapServer instance and attempts to retrieve the Capabilities document specified by serverURL.
     *
     * @param serverURL a URL that points to the capabilities document of a server
     * @param timeout a time to be wait a server response
     * @throws IOException if there is an error communicating with the server
     * @throws ServiceException if the server responds with an error
     */
    public WebMapServer(final URL serverURL, int timeout) throws IOException, ServiceException {
        super(serverURL, getHttpClient(timeout));
    }

    public static HTTPClient getHttpClient(int timeout) {
        HTTPClient client = HTTPClientFinder.createClient();
        client.setReadTimeout(timeout);
        return client;
    }

    /** Sets up the specifications/versions that this server is capable of communicating with. */
    @Override
    protected void setupSpecifications() {
        specs = new Specification[4];
        specs[0] = new WMS1_0_0();
        specs[1] = new WMS1_1_0();
        specs[2] = new WMS1_1_1();
        specs[3] = new WMS1_3_0();
    }

    @Override
    protected ServiceInfo createInfo() {
        return new WMSInfo();
    }

    @Override
    protected ResourceInfo createInfo(Layer layer) {
        return new LayerInfo(layer);
    }

    @Override
    public GetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request) throws IOException, ServiceException {
        return (GetCapabilitiesResponse) internalIssueRequest(request);
    }

    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        return (GetMapResponse) internalIssueRequest(request);
    }

    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request) throws IOException, ServiceException {
        return (GetFeatureInfoResponse) internalIssueRequest(request);
    }

    public DescribeLayerResponse issueRequest(DescribeLayerRequest request) throws IOException, ServiceException {
        return (DescribeLayerResponse) internalIssueRequest(request);
    }

    public GetLegendGraphicResponse issueRequest(GetLegendGraphicRequest request) throws IOException, ServiceException {
        return (GetLegendGraphicResponse) internalIssueRequest(request);
    }

    public GetStylesResponse issueRequest(GetStylesRequest request) throws IOException, ServiceException {
        return (GetStylesResponse) internalIssueRequest(request);
    }

    public PutStylesResponse issueRequest(PutStylesRequest request) throws IOException, ServiceException {
        return (PutStylesResponse) internalIssueRequest(request);
    }

    /**
     * Get the getCapabilities document. If there was an error parsing it during creation, it will return null (and it
     * should have thrown an exception during creation).
     *
     * @return a WMSCapabilities object, representing the Capabilities of the server
     */
    @Override
    public WMSCapabilities getCapabilities() {
        return capabilities;
    }

    private WMSSpecification getSpecification() {
        return (WMSSpecification) specification;
    }

    private URL findURL(OperationType operation) {
        if (operation.getGet() != null) {
            return operation.getGet();
        }
        return serverURL;
    }

    /**
     * Creates a GetMapRequest that can be configured and then passed to issueRequest().
     *
     * @return a configureable GetMapRequest object
     */
    public GetMapRequest createGetMapRequest() {
        URL onlineResource = findURL(getCapabilities().getRequest().getGetMap());

        return getSpecification().createGetMapRequest(onlineResource);
    }

    /**
     * Creates a GetFeatureInfoRequest that can be configured and then passed to issueRequest().
     *
     * @param getMapRequest a previous configured GetMapRequest
     * @return a GetFeatureInfoRequest
     * @throws UnsupportedOperationException if the server does not support GetFeatureInfo
     */
    public GetFeatureInfoRequest createGetFeatureInfoRequest(GetMapRequest getMapRequest) {
        if (getCapabilities().getRequest().getGetFeatureInfo() == null) {
            throw new UnsupportedOperationException("This Web Map Server does not support GetFeatureInfo requests");
        }

        URL onlineResource = findURL(getCapabilities().getRequest().getGetFeatureInfo());

        GetFeatureInfoRequest request = getSpecification().createGetFeatureInfoRequest(onlineResource, getMapRequest);

        return request;
    }

    public DescribeLayerRequest createDescribeLayerRequest() throws UnsupportedOperationException {
        if (getCapabilities().getRequest().getDescribeLayer() == null) {
            throw new UnsupportedOperationException(
                    "Server does not specify a DescribeLayer operation. Cannot be performed");
        }

        URL onlineResource = getCapabilities().getRequest().getDescribeLayer().getGet();
        if (onlineResource == null) {
            onlineResource = serverURL;
        }

        DescribeLayerRequest request = getSpecification().createDescribeLayerRequest(onlineResource);

        return request;
    }

    public GetLegendGraphicRequest createGetLegendGraphicRequest() throws UnsupportedOperationException {
        if (getCapabilities().getRequest().getGetLegendGraphic() == null) {
            throw new UnsupportedOperationException(
                    "Server does not specify a GetLegendGraphic operation. Cannot be performed");
        }

        URL onlineResource =
                getCapabilities().getRequest().getGetLegendGraphic().getGet();
        if (onlineResource == null) {
            onlineResource = serverURL;
        }

        GetLegendGraphicRequest request = getSpecification().createGetLegendGraphicRequest(onlineResource);

        return request;
    }

    public GetStylesRequest createGetStylesRequest() throws UnsupportedOperationException {
        if (getCapabilities().getRequest().getGetStyles() == null) {
            throw new UnsupportedOperationException(
                    "Server does not specify a GetStyles operation. Cannot be performed");
        }

        URL onlineResource = getCapabilities().getRequest().getGetStyles().getGet();
        if (onlineResource == null) {
            onlineResource = serverURL;
        }

        GetStylesRequest request = getSpecification().createGetStylesRequest(onlineResource);

        return request;
    }

    public PutStylesRequest createPutStylesRequest() throws UnsupportedOperationException {
        if (getCapabilities().getRequest().getPutStyles() == null) {
            throw new UnsupportedOperationException(
                    "Server does not specify a PutStyles operation. Cannot be performed");
        }

        URL onlineResource = getCapabilities().getRequest().getPutStyles().getGet();
        if (onlineResource == null) {
            onlineResource = serverURL;
        }

        PutStylesRequest request = getSpecification().createPutStylesRequest(onlineResource);
        return request;
    }

    /**
     * Given a layer and a coordinate reference system, will locate an envelope for that layer in that CRS. If the layer
     * is declared to support that CRS, but no envelope can be found, it will try to calculate an appropriate bounding
     * box.
     *
     * <p>If null is returned, no valid bounding box could be found and one couldn't be transformed from another.
     *
     * @return an Envelope containing a valid bounding box, or null if none are found
     */
    public GeneralBounds getEnvelope(Layer layer, CoordinateReferenceSystem crs) {
        return layer.getEnvelope(crs);
    }
}
