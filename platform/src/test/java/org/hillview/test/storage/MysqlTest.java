/*
 * Copyright (c) 2019 VMware Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hillview.test.storage;

import org.hillview.sketches.DataRange;
import org.hillview.sketches.DoubleHistogramBuckets;
import org.hillview.sketches.Histogram;
import org.hillview.sketches.StringHistogramBuckets;
import org.hillview.storage.JdbcConnectionInformation;
import org.hillview.storage.JdbcDatabase;
import org.hillview.table.ColumnDescription;
import org.hillview.table.Schema;
import org.hillview.table.SmallTable;
import org.hillview.table.api.ContentsKind;
import org.hillview.table.api.IColumn;
import org.hillview.table.api.ITable;
import org.hillview.table.rows.RowSnapshot;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;

public class MysqlTest extends JdbcTest {
    /**
     * Returns a connection information suitable for accessing
     * the table "employees" in the "test_db" mysql database.
     */
    private JdbcConnectionInformation mySqlTestDbConnection() {
        JdbcConnectionInformation conn = new JdbcConnectionInformation();
        conn.databaseKind = "mysql";
        conn.port = 3306;
        conn.host = "localhost";
        conn.database = "employees";
        conn.table = "salaries";
        conn.user = "user";
        conn.password = "password";
        return conn;
    }

    @Test
    public void testMysqlConnection() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        ITable table = this.getTable(conn);
        if (table != null)
            Assert.assertEquals("Table[4x2844047]", table.toString());
    }

    @Test
    public void testMysqlLazy() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        conn.lazyLoading = true;
        ITable table = this.getTable(conn);
        if (table != null) {
            Assert.assertEquals("Table[4x2844047]", table.toString());
            IColumn col = table.getLoadedColumn("salary");
            int firstSalary = col.getInt(0);
            Assert.assertEquals(60117, firstSalary);

            IColumn emp = table.getLoadedColumn("emp_no");
            int empNo = emp.getInt(0);
            Assert.assertEquals(10001, empNo);
        }
    }

    @Test
    public void testMysqlRowCount() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        int rows = db.getRowCount();
        db.disconnect();
        Assert.assertEquals(2844047, rows);
    }

    @Test
    public void testMysqlDistinct() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        int distinct = db.distinctCount("salary");
        db.disconnect();
        Assert.assertEquals(85814, distinct);
    }

    @Test
    public void testMysqlTopK() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        Schema schema = new Schema();
        schema.append(new ColumnDescription("salary", ContentsKind.Double));
        SmallTable tbl = db.topFreq(schema, 10000);
        db.disconnect();
        Assert.assertEquals(1, tbl.getNumOfRows());
        //noinspection MismatchedQueryAndUpdateOfCollection
        RowSnapshot row = new RowSnapshot(tbl, 0);
        String col = row.getColumnNames().get(1);
        Assert.assertEquals(95373, (int)row.getDouble(col));
        Assert.assertEquals(40000, row.getInt("salary"));
    }

    @Test
    public void testMysqlRange() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        DataRange range = db.numericDataRange(new ColumnDescription("salary", ContentsKind.Integer));
        Assert.assertNotNull(range);
        Assert.assertEquals(38623.0, range.min, .1);
        Assert.assertEquals(158220.0, range.max, .1);
        Assert.assertEquals(2844047, range.presentCount);
        Assert.assertEquals(0, range.missingCount);
        db.disconnect();
    }

    @Test
    public void testMysqlNumericHistogram() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        DoubleHistogramBuckets buckets = new DoubleHistogramBuckets(0, 200000, 8);
        Histogram histogram = db.histogram(
                new ColumnDescription("salary", ContentsKind.Integer), buckets);
        Assert.assertNotNull(histogram);
        Assert.assertEquals(8, histogram.getNumOfBuckets());
        Assert.assertEquals(0, histogram.getMissingData());
        Assert.assertEquals(0, histogram.getCount(0));
        Assert.assertEquals(0, histogram.getCount(7));
        long total = 0;
        for (int i = 0; i < histogram.getNumOfBuckets(); i++)
            total += histogram.getCount(i);
        Assert.assertEquals(2844047, total);
        db.disconnect();
    }

    @Test
    public void testMysqlStringHistogram() throws SQLException {
        JdbcConnectionInformation conn = this.mySqlTestDbConnection();
        conn.table = "employees";
        JdbcDatabase db = new JdbcDatabase(conn);
        try {
            db.connect();
        } catch (Exception e) {
            // This will fail if a database is not deployed, but we don't want to fail the test.
            this.ignoringException("Cannot connect to database", e);
            return;
        }
        String[] boundaries = new String[] { "a", "f", "k", "p", "t", "x" };
        StringHistogramBuckets buckets = new StringHistogramBuckets(boundaries);
        Histogram histogram = db.histogram(
                new ColumnDescription("first_name", ContentsKind.String), buckets);
        Assert.assertNotNull(histogram);
        Assert.assertEquals(6, histogram.getNumOfBuckets());
        long total = 0;
        for (int i = 0; i < histogram.getNumOfBuckets(); i++)
            total += histogram.getCount(i);
        Assert.assertEquals(300024, total);
        db.disconnect();
    }
}
