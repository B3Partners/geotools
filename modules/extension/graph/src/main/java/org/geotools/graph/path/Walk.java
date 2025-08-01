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
package org.geotools.graph.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;

/**
 * Represents a walk in a graph. A <B>walk</B> W is defined as an ordered set of nodes that two adjacenct nodes in the
 * set share an edge. More precisley: <br>
 * <br>
 * G = {N,E} W = { n(i) in N | (n(i-1),n(i)) in E }
 *
 * @author Justin Deoliveira, Refractions Research Inc, jdeolive@refractions.net
 */
public class Walk extends ArrayList<Node> implements NodeSequence {

    private List<Edge> m_edges;

    public Walk() {}

    public Walk(Collection<Node> nodes) {
        super(nodes);
    }

    /** A valid walk is one in which each pair of adjacent nodes in the sequence share an edge. Note, */
    @Override
    public boolean isValid() {
        // if edges were calculated successfly it is a valid walk
        return getEdges() != null;
    }

    /**
     * Calculates the edges in the walk. If the edges of the walk cannot be calculated (due to an invalid walk), null is
     * returned, otherwise the list of edges is returned.
     *
     * @return The edges of the walk, otherwise null if the edges cannot be calculated.
     */
    public List<Edge> getEdges() {
        // calculate edges
        if (m_edges == null) {
            m_edges = buildEdges();
        }

        return m_edges;
    }

    /**
     * Adds a node to the walk. Adding a node clears the edge list which will be recalculated on the next call to
     * getEdges().
     *
     * @param node Node to add to the walk.
     */
    @Override
    public boolean add(Node node) {
        m_edges = null;
        return super.add(node);
    }

    @Override
    public void add(int index, Node element) {
        super.add(index, element);
        m_edges = null;
    }

    @Override
    public boolean addAll(Collection<? extends Node> c) {
        m_edges = null;
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Node> c) {
        m_edges = null;
        return super.addAll(index, c);
    }

    public boolean addEdge(Edge e) {
        // append edge to end of path, path must be empty, or last node in path
        // must be a node of the edge

        // save current edge list
        List<Edge> edges = getEdges();

        if (isEmpty()) {
            // add both nodes
            add(e.getNodeA());
            add(e.getNodeB());
        } else {
            // walk is not empty, check to see if the last node is related to the edge
            Node last = getLast();

            if (last.equals(e.getNodeA())) {
                add(e.getNodeB());
            } else if (last.equals(e.getNodeB())) {
                add(e.getNodeA());
            } else return false;
        }

        // the addition of nodes resets the internal edge list so it must be rebuilt.
        // In the case that an edge shares both of its nodes with another edge
        // it is possible for the list to be rebuilt properly (ie. not contain
        // the edge being added). To rectify this situation, a backup copy of the
        // edge list is saved before the addition, the addition performed, the
        // edge explicitly added to the backup edge list, and the internal
        // edge list replaced by the modified backup
        edges.add(e);
        m_edges = edges;

        return true;
    }

    public void addEdges(Collection<Edge> edges) {
        for (Edge e : edges) {
            addEdge(e);
        }
    }

    /**
     * Removes a node from the walk. Removing a node clears the edge list which will be recalculated on the next call to
     * getEdges().
     *
     * @param node Node to remove from the walk.
     */
    public void remove(Node node) {
        super.remove(node);
        m_edges = null;
    }

    @Override
    public Node remove(int index) {
        m_edges = null;
        return super.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        m_edges = null;
        return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection c) {
        m_edges = null;
        return super.removeAll(c);
    }

    /**
     * Determines if the walk is closed. A closed walk is one in which the first and last nodes are the same.
     *
     * @return True if closed, otherwise false.
     */
    public boolean isClosed() {
        if (isEmpty() || !isValid()) return false;
        return get(0).equals(get(size() - 1));
    }

    /** @see NodeSequence#getFirst() */
    @Override
    public Node getFirst() {
        return get(0);
    }

    /** @see NodeSequence#getLast() */
    @Override
    public Node getLast() {
        return get(size() - 1);
    }

    /**
     * Internal method for building the edge set of the walk. This method calculated the edges upon every call.
     *
     * @return The list of edges for the walk, or null if the edge set could not be calculated due to an invalid walk.
     */
    protected List<Edge> buildEdges() {
        List<Edge> edges = new ArrayList<>();

        for (int i = 1; i < size(); i++) {
            Node prev = get(i - 1);
            Node curr = get(i);

            Edge e = curr.getEdge(prev);

            if (e != null) edges.add(e);
            else return null;
        }

        return edges;
    }

    /** Reverses the path. */
    public void reverse() {
        Collections.reverse(this);
        m_edges = null;
    }

    /**
     * Truncates the path at the specified index. Nodes in the path whose index is >= the specified index are removed.
     *
     * @param index The index of first node to be removed.
     */
    public void truncate(int index) {
        removeRange(index, size());
        m_edges = null;
    }

    /**
     * Returns an iterator that iterates over the path in reverse. The iterator does not support the remove operation.
     *
     * @return the reverse iterator.
     */
    public Iterator<Node> riterator() {
        return new Iterator<>() {
            int m_index = size() - 1;

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Path iterator does not support remove()");
            }

            @Override
            public boolean hasNext() {
                return m_index > -1;
            }

            @Override
            public Node next() {
                return get(m_index--);
            }
        };
    }

    public Path duplicate() {
        return new Path(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Walk) return equals((Walk) other);
        return false;
    }

    public boolean equals(Walk other) {
        if (other.size() == size()) {
            // make a node by node comparision
            Iterator<Node> thisnodes = iterator();
            Iterator<Node> othernodes = other.iterator();

            while (thisnodes.hasNext()) {
                Node thisnode = thisnodes.next();
                Node othernode = othernodes.next();

                if (!thisnode.equals(othernode)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getFirst().hashCode();
        hash = 31 * hash + getLast().hashCode();
        return hash;
    }
}
