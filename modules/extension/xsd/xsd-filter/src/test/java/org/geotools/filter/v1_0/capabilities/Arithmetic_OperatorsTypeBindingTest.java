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
package org.geotools.filter.v1_0.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import org.geotools.api.filter.capability.ArithmeticOperators;
import org.geotools.xsd.Binding;
import org.junit.Test;
import org.w3c.dom.Document;

public class Arithmetic_OperatorsTypeBindingTest extends FilterCapabilitiesTestSupport {

    @Test
    public void testType() {
        assertEquals(
                ArithmeticOperators.class, binding(OGC.Arithmetic_OperatorsType).getType());
    }

    @Test
    public void testExectionMode() {
        assertEquals(Binding.OVERRIDE, binding(OGC.Arithmetic_OperatorsType).getExecutionMode());
    }

    @Test
    public void testParse1() throws Exception {
        FilterMockData.arithmetic(document, document);

        ArithmeticOperators arithmetic = (ArithmeticOperators) parse(OGC.Arithmetic_OperatorsType);

        assertTrue(arithmetic.hasSimpleArithmetic());
        assertNotNull(arithmetic.getFunctions());
    }

    @Test
    public void testParse2() throws Exception {
        FilterMockData.arithmetic(document, document, false);

        ArithmeticOperators arithmetic = (ArithmeticOperators) parse(OGC.Arithmetic_OperatorsType);

        assertFalse(arithmetic.hasSimpleArithmetic());
        assertNotNull(arithmetic.getFunctions());
    }

    @Test
    public void testEncode() throws Exception {
        Document dom = encode(
                FilterMockData.arithmetic(true),
                new QName(OGC.NAMESPACE, "Arithmetic_Operators"),
                OGC.Arithmetic_OperatorsType);

        assertNotNull(getElementByQName(dom, OGC.Simple_Arithmetic));
        assertNotNull(getElementByQName(dom, new QName(OGC.NAMESPACE, "Functions")));

        dom = encode(
                FilterMockData.arithmetic(false),
                new QName(OGC.NAMESPACE, "Arithmetic_Operators"),
                OGC.Arithmetic_OperatorsType);
        assertNull(getElementByQName(dom, OGC.Simple_Arithmetic));
        assertNotNull(getElementByQName(dom, new QName(OGC.NAMESPACE, "Functions")));
    }
}
