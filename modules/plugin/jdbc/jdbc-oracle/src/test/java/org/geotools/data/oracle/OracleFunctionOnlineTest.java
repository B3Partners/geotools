/*
 * Copyright (C) 2022 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */
package org.geotools.data.oracle;

import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.util.NullProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jdbc.JDBCFunctionOnlineTest;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.opengis.filter.sort.SortOrder;

public class OracleFunctionOnlineTest extends JDBCFunctionOnlineTest {
    static final Logger LOGGER = Logging.getLogger(OracleFunctionOnlineTest.class);

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new OracleFunctionTestSetup();
    }

    @After
    public void tearDownDates() {
        ((OracleFunctionTestSetup) this.setup).cleanUpNonUniqueDatesTable();
    }

    /**
     * Test the Collection_Unique function on an Oracle Timestamp column.
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void testUniqueDates() throws Exception {
        ((OracleFunctionTestSetup) this.setup).createNonUniqueDatesTable();

        final String attributeName = aname("d");
        final String tableName = tname("nonuniquedates");

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        FeatureSource<SimpleFeatureType, SimpleFeature> fs = dataStore.getFeatureSource(tableName);

        Filter notNull = ff.not(ff.isNull(ff.property(attributeName)));
        Query notNullAndSortedQuery = new Query(tableName, notNull);
        notNullAndSortedQuery.setPropertyNames(attributeName);
        notNullAndSortedQuery.setSortBy(ff.sort(attributeName, SortOrder.ASCENDING));

        // test with a Function
        Function uniqueFuntion = ff.function("Collection_Unique", ff.property(attributeName));
        Object evaluated = uniqueFuntion.evaluate(fs.getFeatures(notNullAndSortedQuery));
        assertEquals(
                "Expect unique values to be represented as a Set implementation",
                Set.class,
                evaluated.getClass().getInterfaces()[0]);
        assertEquals("Expect 3 unique values", 3, ((Set<?>) evaluated).size());

        ((Set<?>) evaluated)
                .forEach(
                        dt -> {
                            LOGGER.log(
                                    Level.FINE,
                                    "Got class {0}, value {1} using function",
                                    new Object[] {dt.getClass().getName(), dt});
                            assertEquals(
                                    "Expect each unique value to be represented as a date",
                                    Timestamp.class,
                                    dt.getClass());
                        });

        // test with a FeatureVisitor
        HashSet<Object> result = new HashSet<>();
        fs.getFeatures(notNullAndSortedQuery)
                .accepts(
                        feature -> result.add(feature.getProperty(attributeName).getValue()),
                        new NullProgressListener());

        assertEquals("Expect 3 unique values", 3, result.size());
        result.forEach(
                dt -> {
                    LOGGER.log(
                            Level.FINE,
                            "Got class {0}, value {1} using visitor",
                            new Object[] {dt.getClass().getName(), dt});
                    assertEquals(
                            "Expect each unique value to be represented as a date",
                            Date.class,
                            dt.getClass());
                });
    }
}
