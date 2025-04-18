/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013 - 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.renderer.lite;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.label.LabelCacheImpl;
import org.geotools.test.TestData;
import org.junit.Before;
import org.junit.Test;

/** Created by Michaël on 17/12/13. */
public class PartialsTest {

    SimpleFeatureSource fs_point;
    SimpleFeatureSource fs_line;
    SimpleFeatureSource fs_area;
    ReferencedEnvelope bounds;
    StreamingRenderer renderer;

    @Before
    public void setUp() throws Exception {

        File property_point = new File(
                TestData.getResource(this, "partialPointLabel.properties").toURI());
        PropertyDataStore ds_point = new PropertyDataStore(property_point.getParentFile());
        fs_point = ds_point.getFeatureSource("partialPointLabel");

        File property_line = new File(
                TestData.getResource(this, "partialLineLabel.properties").toURI());
        PropertyDataStore ds_line = new PropertyDataStore(property_line.getParentFile());
        fs_line = ds_line.getFeatureSource("partialLineLabel");

        File property_area = new File(
                TestData.getResource(this, "partialAreaLabel.properties").toURI());
        PropertyDataStore ds_area = new PropertyDataStore(property_area.getParentFile());
        fs_area = ds_area.getFeatureSource("partialAreaLabel");

        bounds = new ReferencedEnvelope(0, 10, 0, 10, DefaultGeographicCRS.WGS84);

        renderer = new StreamingRenderer();
        Map<Object, Object> rendererParams = new HashMap<>();
        LabelCacheImpl labelCache = new LabelCacheImpl();
        rendererParams.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache);
        renderer.setRendererHints(rendererParams);
        renderer.setJava2DHints(new RenderingHints(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON));
    }

    /*
    public void testLabelNatural() throws Exception {
        Style style = RendererBaseTest.loadStyle(this, "partialPointLabelNo.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_point, style));

        renderer.setMapContent(mc);

        RendererBaseTest.showRender("Lines with circle stroke", renderer, TIME, bounds);
    }
    */

    @Test
    public void testPartialPointLabelNo() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialPointLabelNo.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_point, style));

        renderer.setMapContent(mc);
        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 152, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialPointLabelNo.png"));
        // Interactive visualization
        // PartialsTest.showImage("Point Partials:No", TIME, image);
    }

    @Test
    public void testPointLabelFalse() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialPointLabelFalse.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_point, style));

        renderer.setMapContent(mc);
        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 152, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialPointLabelFalse.png"));
        // Interactive visualization
        // PartialsTest.showImage("Point Partials:False", TIME, image);
    }

    @Test
    public void testPartialPointLabelTrue() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialPointLabelTrue.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_point, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 150, Color.BLACK);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialPointLabelTrue.png"));
        // Interactive visualization
        // PartialsTest.showImage("Point Partials:True", TIME, image);
    }

    @Test
    public void testPartialLineLabelNo() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialLineLabelNo.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_line, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 155, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialLineLabelNo.png"));
        // Interactive visualization
        // PartialsTest.showImage("Line Partials:No", TIME, image);
    }

    @Test
    public void testPartialLineLabelFalse() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialLineLabelFalse.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_line, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 155, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialLineLabelFalse.png"));
        // Interactive visualization
        // PartialsTest.showImage("Line Partials:False", TIME, image);
    }

    @Test
    public void testPartialLineLabelTrue() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialLineLabelTrue.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_line, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        // RenderedImageBrowser.showChain(image);
        RendererBaseTest.assertPixel(image, 150, 1, Color.BLACK, 30);
    }

    @Test
    public void testPartialAreaLabelNo() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialAreaLabelNo.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_area, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 152, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialAreaLabelNo.png"));
        // Interactive visualization
        // PartialsTest.showImage("Area Partials:No", TIME, image);
    }

    @Test
    public void testPartialAreaLabelFalse() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialAreaLabelFalse.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_area, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 152, Color.WHITE);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialAreaLabelFalse.png"));
        // Interactive visualization
        // PartialsTest.showImage("Area Partials:False", TIME, image);
    }

    @Test
    public void testPartialAreaLabelTrue() throws Exception {
        // System.setProperty("java.awt.headless", "false");
        Thread.sleep(1000);
        Style style = RendererBaseTest.loadStyle(this, "partialAreaLabelTrue.sld");

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(fs_area, style));

        renderer.setMapContent(mc);

        final BufferedImage image = RendererBaseTest.renderImage(renderer, bounds, null);
        RendererBaseTest.assertPixel(image, 282, 150, Color.BLACK);

        // Write to file
        // ImageIO.write(image, "png", new File("C:/Temp/testPartialAreaLabelTrue.png"));
        // Interactive visualization
        // PartialsTest.showImage("Area Partials:True", TIME, image);
    }

    public static void showImage(String testName, long timeOut, final BufferedImage image) throws InterruptedException {
        // final String headless = System.getProperty("java.awt.headless", "false");
        // if (!headless.equalsIgnoreCase("true") && TestData.isInteractiveTest()) {
        try {
            Frame frame = new Frame(testName);
            frame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    e.getWindow().dispose();
                }
            });

            Panel p = new Panel() {

                /** <code>serialVersionUID</code> field */
                private static final long serialVersionUID = 1L;

                {
                    setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                }

                @Override
                public void paint(Graphics g) {
                    g.drawImage(image, 0, 0, this);
                }
            };

            frame.add(p);
            frame.pack();
            frame.setVisible(true);

            Thread.sleep(timeOut);
            frame.dispose();
        } catch (HeadlessException exception) {
            // The test is running on a machine without X11 display. Ignore.
        }
        // }
    }
}
