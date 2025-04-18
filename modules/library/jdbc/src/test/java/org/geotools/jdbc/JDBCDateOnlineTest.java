package org.geotools.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.junit.Test;

public abstract class JDBCDateOnlineTest extends JDBCTestSupport {

    @Override
    protected abstract JDBCDateTestSetup createTestSetup();

    @Test
    public void testMappings() throws Exception {
        SimpleFeatureType ft = dataStore.getSchema(tname("dates"));

        assertEquals(Date.class, ft.getDescriptor(aname("d")).getType().getBinding());
        assertEquals(Timestamp.class, ft.getDescriptor(aname("dt")).getType().getBinding());
        assertEquals(Time.class, ft.getDescriptor(aname("t")).getType().getBinding());
    }

    @Test
    public void testFiltersByDate() throws Exception {

        boolean simple = useOneTimeZoneOnly();
        FilterFactory ff = dataStore.getFilterFactory();

        DateFormat df = new SimpleDateFormat("yyyy-dd-MM");
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone[] zones = {
            TimeZone.getTimeZone("Etc/GMT+12"),
            TimeZone.getTimeZone("PST"),
            TimeZone.getTimeZone("EST"),
            TimeZone.getTimeZone("GMT"),
            TimeZone.getTimeZone("CET"),
            TimeZone.getTimeZone("Etc/GMT-12"),
            TimeZone.getTimeZone("Etc/GMT-14")
        };
        if (simple) {
            zones = new TimeZone[1];
            zones[0] = originalTimeZone;
        }
        try {
            for (TimeZone zone : zones) {

                FeatureSource fs = dataStore.getFeatureSource(tname("dates"));

                // set JVM time zone
                TimeZone.setDefault(zone);
                // regenerate the database table using the new JVM Timezone

                setup.setUpData();
                df.setTimeZone(zone);
                // less than
                Filter f = ff.lessOrEqual(ff.property(aname("d")), ff.literal(df.parse("2009-28-06")));
                // System.out.println(f);
                assertEquals(
                        "wrong number of records for " + zone.getDisplayName(),
                        2,
                        fs.getCount(new Query(tname("dates"), f)));
                /*
                            f = ff.lessOrEqual(ff.property(aname("d")),
                                    ff.literal(df.parse("2009-28-06")));
                            assertEquals(
                                    "wrong number of records for " + zone.getDisplayName(), 2,
                                    fs.getCount(new Query(tname("dates"), f)));
                */
            }
        } finally {
            // set JVM time zone
            TimeZone.setDefault(originalTimeZone);
            // regenerate the database table using the new JVM Timezone

            setup.setUpData();
        }
    }

    @Test
    public void testFilterByTimeStamp() throws Exception {
        FeatureSource fs = dataStore.getFeatureSource(tname("dates"));

        FilterFactory ff = dataStore.getFilterFactory();

        // equal to
        Filter f = ff.equals(ff.property(aname("dt")), ff.literal("2009-06-28 15:12:41"));
        assertEquals(1, fs.getCount(new Query(tname("dates"), f)));

        f = ff.equals(
                ff.property(aname("dt")),
                ff.literal(new SimpleDateFormat("HH:mm:ss,dd-yyyy-MM").parse("15:12:41,28-2009-06")));
        assertEquals(1, fs.getCount(new Query(tname("dates"), f)));
    }

    @Test
    public void testFilterByTime() throws Exception {
        FeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        FilterFactory ff = dataStore.getFilterFactory();

        // greater than or equal to
        Filter f = ff.greaterOrEqual(ff.property(aname("t")), ff.literal("13:10:12"));
        assertEquals(3, fs.getCount(new Query(tname("dates"), f)));

        f = ff.greaterOrEqual(ff.property(aname("t")), ff.literal(new SimpleDateFormat("ss:HH:mm").parse("12:13:10")));
        assertEquals(3, fs.getCount(new Query(tname("dates"), f)));
    }

    protected boolean useOneTimeZoneOnly() {
        // Some JDBC drivers cache the default time zone when a statement is prepared.
        // Prepared statement pooling (enabled by default) will prevent the driver of querying the
        // default time zone again after it has been changed. So, for prepared statement dialects,
        // we will use one time zone only by default.
        return dataStore.dialect instanceof PreparedStatementSQLDialect;
    }
}
