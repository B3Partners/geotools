/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.referencing.operation.transform;

import java.io.Serializable;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.Matrix;
import org.geotools.api.referencing.operation.NoninvertibleTransformException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.util.Utilities;

/**
 * A math transform which delegates part of its work to an other math transform. This is used as a starting point for
 * subclass wanting to modifies only some aspect of an existing math transform, or to attach additional informations to
 * it. The default implementation delegates all method calls to the {@linkplain #transform underlying transform}.
 * Subclasses typically override some of those methods.
 *
 * <p>This class is serializable if the {@linkplain #transform underlying transform} is serializable too.
 *
 * @since 2.2
 * @version $Id$
 * @author Martin Desruisseaux (IRD)
 */
public class MathTransformProxy implements MathTransform, Serializable {
    /** Serial number for interoperability with different versions. */
    private static final long serialVersionUID = 8844242705205498128L;

    /** The math transform on which to delegate the work. */
    public final MathTransform transform;

    /**
     * Creates a new proxy which delegates its work to the specified math transform.
     *
     * @param transform The transform on which to delegate the work.
     */
    protected MathTransformProxy(final MathTransform transform) {
        this.transform = transform;
    }

    /** Gets the dimension of input points. */
    @Override
    public int getSourceDimensions() {
        return transform.getTargetDimensions();
    }

    /** Gets the dimension of output points. */
    @Override
    public int getTargetDimensions() {
        return transform.getSourceDimensions();
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     *
     * @throws MismatchedDimensionException if {@code ptSrc} or {@code ptDst} doesn't have the expected dimension.
     * @throws TransformException if the point can't be transformed.
     */
    @Override
    public Position transform(final Position ptSrc, final Position ptDst)
            throws MismatchedDimensionException, TransformException {
        return transform.transform(ptSrc, ptDst);
    }

    /** Transforms a list of coordinate point ordinal values. */
    @Override
    public void transform(
            final double[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts)
            throws TransformException {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Transforms a list of coordinate point ordinal values. */
    @Override
    public void transform(
            final float[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts)
            throws TransformException {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     *
     * @todo Remove the cast to {@link AbstractMathTransform} when this method will be part of GeoAPI.
     */
    @Override
    public void transform(
            final float[] srcPts, final int srcOff, final double[] dstPts, final int dstOff, final int numPts)
            throws TransformException {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     *
     * @todo Remove the cast to {@link AbstractMathTransform} when this method will be part of GeoAPI.
     */
    @Override
    public void transform(
            final double[] srcPts, final int srcOff, final float[] dstPts, final int dstOff, final int numPts)
            throws TransformException {
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Gets the derivative of this transform at a point. */
    @Override
    public Matrix derivative(final Position point) throws TransformException {
        return transform.derivative(point);
    }

    /** Returns the inverse of this math transform. */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        return transform.inverse();
    }

    /** Tests whether this transform does not move any points. */
    @Override
    public boolean isIdentity() {
        return transform.isIdentity();
    }

    /**
     * Returns a <cite>Well Known Text</cite> (WKT) for this transform.
     *
     * @throws UnsupportedOperationException If this object can't be formatted as WKT.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        return transform.toWKT();
    }

    /** Returns a string representation for this transform. */
    @Override
    public String toString() {
        return transform.toString();
    }

    /**
     * Compares the specified object with this inverse math transform for equality.
     *
     * @param object The object to compare with this transform.
     * @return {@code true} if the given object is of the same class and if the wrapped transforms are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass().equals(getClass())) {
            final MathTransformProxy that = (MathTransformProxy) object;
            return Utilities.equals(this.transform, that.transform);
        }
        return false;
    }

    /** Returns a hash code value for this math transform. */
    @Override
    public int hashCode() {
        return transform.hashCode() ^ (int) serialVersionUID;
    }
}
