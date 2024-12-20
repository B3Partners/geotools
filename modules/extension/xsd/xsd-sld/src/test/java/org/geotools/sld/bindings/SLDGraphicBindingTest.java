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
package org.geotools.sld.bindings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Color;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.Mark;
import org.geotools.filter.Filters;
import org.junit.Test;

public class SLDGraphicBindingTest extends SLDTestSupport {
    @Test
    public void testType() throws Exception {
        assertEquals(Graphic.class, new SLDGraphicBinding(null).getType());
    }

    @Test
    public void testGraphic() throws Exception {
        SLDMockData.graphic(document, document);

        Graphic graphic = (Graphic) parse();
        assertNotNull(graphic);

        assertEquals(graphic.graphicalSymbols().size(), 1);

        Mark mark = (Mark) graphic.graphicalSymbols().get(0);

        Color c = org.geotools.styling.SLD.color(mark.getFill().getColor());
        assertEquals(Integer.parseInt("12", 16), c.getRed());
        assertEquals(Integer.parseInt("34", 16), c.getGreen());
        assertEquals(Integer.parseInt("56", 16), c.getBlue());

        assertEquals(1, Filters.asInt(graphic.getSize()));
        assertEquals(1, Filters.asInt(graphic.getOpacity()));
        assertEquals(90, Filters.asInt(graphic.getRotation()));
    }

    @Test
    public void testGraphicEmptyRotation() throws Exception {
        SLDMockData.graphicWithEmptyRotation(document, document);
        Graphic graphic = (Graphic) parse();
        assertNotNull(graphic);

        assertEquals(graphic.graphicalSymbols().size(), 1);

        Mark mark = (Mark) graphic.graphicalSymbols().get(0);

        Color c = org.geotools.styling.SLD.color(mark.getFill().getColor());
        assertEquals(Integer.parseInt("12", 16), c.getRed());
        assertEquals(Integer.parseInt("34", 16), c.getGreen());
        assertEquals(Integer.parseInt("56", 16), c.getBlue());

        assertEquals(1, Filters.asInt(graphic.getSize()));
        assertEquals(1, Filters.asInt(graphic.getOpacity()));
        assertEquals(0, Filters.asInt(graphic.getRotation()));
    }

    @Test
    public void testGraphicEmptySize() throws Exception {
        SLDMockData.graphicWithEmptySize(document, document);
        Graphic graphic = (Graphic) parse();
        assertNotNull(graphic);

        assertEquals(graphic.graphicalSymbols().size(), 1);

        Mark mark = (Mark) graphic.graphicalSymbols().get(0);

        Color c = org.geotools.styling.SLD.color(mark.getFill().getColor());
        assertEquals(Integer.parseInt("12", 16), c.getRed());
        assertEquals(Integer.parseInt("34", 16), c.getGreen());
        assertEquals(Integer.parseInt("56", 16), c.getBlue());

        assertEquals(-1, Filters.asInt(graphic.getSize()));
        assertEquals(1, Filters.asInt(graphic.getOpacity()));
        assertEquals(90, Filters.asInt(graphic.getRotation()));
    }

    @Test
    public void testGraphicEmptyOpacity() throws Exception {
        SLDMockData.graphicWithEmptyOpacity(document, document);
        Graphic graphic = (Graphic) parse();
        assertNotNull(graphic);

        assertEquals(graphic.graphicalSymbols().size(), 1);

        Mark mark = (Mark) graphic.graphicalSymbols().get(0);

        Color c = org.geotools.styling.SLD.color(mark.getFill().getColor());
        assertEquals(Integer.parseInt("12", 16), c.getRed());
        assertEquals(Integer.parseInt("34", 16), c.getGreen());
        assertEquals(Integer.parseInt("56", 16), c.getBlue());

        assertEquals(1, Filters.asInt(graphic.getSize()));
        assertEquals(1, Filters.asInt(graphic.getOpacity()));
        assertEquals(90, Filters.asInt(graphic.getRotation()));
    }
}
