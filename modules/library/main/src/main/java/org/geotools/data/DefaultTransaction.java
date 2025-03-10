/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.Transaction;

/**
 * Quick implementation of Transaction api.
 *
 * <p>Please see Transaction interface for an outline of what this class is all about.
 *
 * @author Jody Garnett, Refractions Research
 */
public class DefaultTransaction implements Transaction {
    /** The logger for the data module. */
    protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(DefaultTransaction.class);

    /** Records State by key */
    Map<Object, Object> stateLookup = new HashMap<>();

    /** Records properties by key */
    Map<Object, Object> propertyLookup = new HashMap<>();

    /** Handle used to identify Transaction for the user */
    String handle;

    /** Records current Authorizations */
    Set<String> authorizations = new HashSet<>();

    public DefaultTransaction() {
        Throwable t = new Throwable("who called me?");
        StackTraceElement e = t.getStackTrace()[1];
        handle = e.getClassName() + "." + e.getMethodName() + " Transaction";
    }

    public DefaultTransaction(String handle) {
        this.handle = handle;
    }

    /**
     * Remembers Externalized State for a DataSource.
     *
     * <p>This is the GOF Momento pattern: a SimpleFeatureSource is able to externalize its internal State required for
     * Transaction support and have this class manage it. It may retrieve this State with getState( key ).
     *
     * <p>In addition several SimpleFeatureSource implementations may share State, a common example is JDBCDataSources
     * keeping a shared JDBC connection using the JDBC URL as a key.
     *
     * @param key Key used to externalize State
     * @param state Externalized State (Momeneto)
     * @throws IllegalArgumentException When Transaction already using key
     * @see Transaction#putState(java.lang.Object, Transaction.State)
     */
    @Override
    public void putState(Object key, State state) {
        if (stateLookup == null) {
            return;
        }
        if (stateLookup.containsKey(key)) {
            State current = (State) stateLookup.get(key);

            if (state == current) {
                throw new IllegalArgumentException("Transaction "
                        + handle
                        + " already has an this State for key: "
                        + key
                        + ". Please check for existing State before creating your own.");
            } else {
                throw new IllegalArgumentException("Transaction "
                        + handle
                        + " already has an entry for key:"
                        + key
                        + ". Please check for existing State before creating your own: "
                        + current);
            }
        } else {
            stateLookup.put(key, state);

            // allow configuration
            state.setTransaction(this);
        }
    }

    /**
     * Removes state from DefaultTransaction's care.
     *
     * <p>Currently does not complain if there is no State associated with key to remove - this may change in the
     * future.
     *
     * @throws IllegalArgumentException If no State was maintained for supplied <code>key</code>
     * @see Transaction#removeState(java.lang.Object)
     */
    @Override
    public void removeState(Object key) {
        if (stateLookup == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        if (stateLookup.containsKey(key)) {
            State state = (State) stateLookup.remove(key);
            state.setTransaction(null);
        } else {
            throw new IllegalArgumentException(
                    "Transaction does not no anything about key:" + key + ". Has this key already been removed?");
        }
    }

    /**
     * Returns externalized state or <code>null</code> if not available.
     *
     * <p>Used by DataStore implementations to externalize information required for Transaction support using the GOF
     * Momento pattern.
     *
     * @return Previously externalized State.
     * @see Transaction#getState(java.lang.Object)
     */
    @Override
    public State getState(Object key) {
        if (stateLookup == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        return (State) stateLookup.get(key);
    }

    /**
     * Commits all modifications against this Transaction.
     *
     * <p>This implementation will call commit() on all State managed by this Transaction. This allows DataStores to
     * provide their own implementation of commit().
     *
     * @throws IOException Encountered problem maintaining transaction state
     * @throws DataSourceException See IOException
     * @see Transaction#commit()
     */
    @Override
    public void commit() throws IOException {
        State state;
        int problemCount = 0;
        IOException io = null;

        for (Object o : stateLookup.values()) {
            state = (State) o;

            try {
                state.commit();
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }

            throw new DataSourceException("Commit encountered " + problemCount + " problems - the first was", io);
        }
        authorizations.clear();
    }

    /**
     * Rollsback all modifications against this Transaction.
     *
     * <p>This implementation will call rollback() on all State managed by this Transaction. This allows DataStores to
     * provide their own implementation of rollback().
     *
     * @throws IOException Encountered problem maintaining transaction State
     * @throws DataSourceException IOException
     * @see Transaction#rollback()
     */
    @Override
    public void rollback() throws IOException {
        int problemCount = 0;
        IOException io = null;
        State state;

        for (Object o : stateLookup.values()) {
            state = (State) o;

            try {
                state.rollback();
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }

            throw new DataSourceException("Rollback encountered " + problemCount + " problems - the first was", io);
        }
        authorizations.clear();
    }

    /** Frees all State held by this Transaction. */
    @Override
    public synchronized void close() {
        for (Object o : stateLookup.values()) {
            State state = (State) o;
            state.setTransaction(null);
        }
        stateLookup.clear();
        stateLookup = null;
        authorizations.clear();
        authorizations = null;
        propertyLookup.clear();
        propertyLookup = null;
    }

    /**
     * The current set of Authorization IDs held by this Transaction.
     *
     * <p>This set is reset by the next call to commit or rollback.
     *
     * @return Set of Authorization IDs
     */
    @Override
    public Set<String> getAuthorizations() {
        if (authorizations == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        return Collections.unmodifiableSet(authorizations);
    }

    /**
     * Provides an authorization ID allowing access to locked Features.
     *
     * <p>Remember authorizations are cleared after every commit/rollback.
     *
     * @param authID Provided Authorization ID
     * @throws IOException Encountered problems maintaing Transaction State
     * @throws DataSourceException See IOException
     * @see Transaction#setAuthorization(java.lang.String)
     */
    @Override
    public void addAuthorization(String authID) throws IOException {
        if (authorizations == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        int problemCount = 0;
        IOException io = null;
        State state;
        authorizations.add(authID);

        for (Object o : stateLookup.values()) {
            state = (State) o;

            try {
                state.addAuthorization(authID);
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }
            throw new DataSourceException(
                    "setAuthorization encountered " + problemCount + " problems - the first was", io);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return handle;
    }

    /**
     * Implementation of getProperty.
     *
     * @see Transaction#getProperty(java.lang.Object)
     */
    @Override
    public Object getProperty(Object key) {
        if (propertyLookup == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        return propertyLookup.get(key);
    }

    /**
     * Implementation of addProperty.
     *
     * @see Transaction#addProperty(java.lang.Object, java.lang.Object)
     */
    @Override
    public void putProperty(Object key, Object value) throws IOException {
        if (propertyLookup == null) {
            throw new IllegalStateException("Transaction has been closed");
        }
        propertyLookup.put(key, value);
    }

    @Override
    @SuppressWarnings("deprecation") // finalize is deprecated in Java 9
    protected void finalize() throws Throwable {
        if (stateLookup != null) {
            LOGGER.severe("There's code leaving transaction unclosed. "
                    + "Call Transaction.close() after using them to ensure they do not hold state "
                    + "such as JDBC connections or file handles");
            LOGGER.severe("Unclosed transaction handle is '" + handle + "'");
            close();
        }
        super.finalize();
    }
}
