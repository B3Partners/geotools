/*
 * Copyright (C) 2022 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */
package org.geotools.data.oracle;

import org.geotools.jdbc.JDBCDataStore;

public class OracleFunctionTestSetup extends OracleTestSetup {

    @Override
    protected void setUpDataStore(JDBCDataStore dataStore) {
        super.setUpDataStore(dataStore);
    }

    void createNonUniqueDatesTable() throws Exception {
        run(
                "CREATE TABLE nonuniquedates (id NUMERIC(10) PRIMARY KEY, d DATE, t TIMESTAMP, ts TIMESTAMP)");

        run(
                "INSERT INTO nonuniquedates VALUES ("
                        + "1, "
                        + "TO_DATE('2009-06-28', 'yyyy-MM-dd'), "
                        + "TO_DATE('2009-06-28 15:12:41', 'yyyy-MM-dd HH24:mi:ss'),"
                        + "TO_DATE('15:12:41', 'HH24:mi:ss')  )");

        run(
                "INSERT INTO nonuniquedates VALUES ("
                        + "2, "
                        + "TO_DATE('2009-01-15', 'yyyy-MM-dd'), "
                        + "TO_DATE('2009-01-15 13:10:12', 'yyyy-MM-dd HH24:mi:ss'),"
                        + "TO_DATE('13:10:12', 'HH24:mi:ss')  )");

        run(
                "INSERT INTO nonuniquedates VALUES ("
                        + "3, "
                        + "TO_DATE('2009-09-29', 'yyyy-MM-dd'), "
                        + "TO_DATE('2009-09-29 17:54:23', 'yyyy-MM-dd HH24:mi:ss'),"
                        + "TO_DATE('17:54:23', 'HH24:mi:ss')  )");
        run(
                "INSERT INTO nonuniquedates VALUES ("
                        + "4, "
                        + "TO_DATE('2009-01-15', 'yyyy-MM-dd'), "
                        + "TO_DATE('2009-01-15 13:10:12', 'yyyy-MM-dd HH24:mi:ss'),"
                        + "TO_DATE('13:10:12', 'HH24:mi:ss')  )");

        run(
                "INSERT INTO nonuniquedates VALUES ("
                        + "5, "
                        + "TO_DATE('2009-09-29', 'yyyy-MM-dd'), "
                        + "TO_DATE('2009-09-29 17:54:23', 'yyyy-MM-dd HH24:mi:ss'),"
                        + "TO_DATE('17:54:23', 'HH24:mi:ss')  )");
    }

    void cleanUpNonUniqueDatesTable() {
        runSafe("DROP TABLE nonuniquedates");
    }
}
