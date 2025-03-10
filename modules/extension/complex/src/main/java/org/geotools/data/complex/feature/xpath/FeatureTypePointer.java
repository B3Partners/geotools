/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.complex.feature.xpath;

import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.feature.type.Types;

/**
 * Special node pointer for {@link org.geotools.feature.FeatureType}.
 *
 * @author Niels Charlier (Curtin University of Technology)
 */
public class FeatureTypePointer extends NodePointer {

    /** */
    private static final long serialVersionUID = 7329150854098309040L;

    /** The name of the node. */
    protected QName name;

    /** The underlying feature type */
    protected ComplexType featureType;

    protected FeatureTypePointer(NodePointer parent, ComplexType featureType, QName name) {
        super(parent);
        this.name = name;
        this.featureType = featureType;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public Object getBaseValue() {
        return null;
    }

    @Override
    public Object getImmediateNode() {
        return featureType;
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException("Feature types are immutable");
    }

    @Override
    public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
        return 0;
    }

    @Override
    public NodeIterator childIterator(NodeTest test, boolean reverse, NodePointer startWith) {
        if (test instanceof NodeNameTest) {
            NodeNameTest nodeNameTest = (NodeNameTest) test;

            if (!nodeNameTest.isWildcard()) {
                String localName = nodeNameTest.getNodeName().getName();
                String nameSpace = nodeNameTest.getNamespaceURI();
                if (nameSpace == null) nameSpace = getNamespaceResolver().getNamespaceURI("");

                return new SingleFeatureTypeAttributeIterator(this, featureType, Types.typeName(nameSpace, localName));
            } else {
                return new FeatureTypeAttributeIterator(this, featureType);
            }
        }

        if (test instanceof NodeTypeTest) {
            NodeTypeTest nodeTypeTest = (NodeTypeTest) test;

            if (nodeTypeTest.getNodeType() == Compiler.NODE_TYPE_NODE) {
                return new FeatureTypeAttributeIterator(this, featureType);
            }
        }

        return super.childIterator(test, reverse, startWith);
    }
}
