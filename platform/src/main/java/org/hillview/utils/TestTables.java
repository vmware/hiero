/*
 * Copyright (c) 2017 VMware Inc. All Rights Reserved.
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
 *
 */

package org.hillview.utils;

import org.hillview.dataset.LocalDataSet;
import org.hillview.dataset.ParallelDataSet;
import org.hillview.dataset.api.IDataSet;
import org.hillview.table.*;
import org.hillview.table.api.*;
import org.hillview.table.columns.CategoryArrayColumn;
import org.hillview.table.columns.DoubleArrayColumn;
import org.hillview.table.columns.IntArrayColumn;
import org.hillview.table.columns.StringArrayColumn;
import org.jblas.DoubleMatrix;
import org.junit.Assert;

import java.util.*;

/**
 * This class generates some constant tables for testing purposes.
 */
public class TestTables {
    /**
     * Can be used for testing.
     * @return A small table with some interesting contents.
     */
    public static Table testTable() {
        ColumnDescription c0 = new ColumnDescription("Name", ContentsKind.Category, false);
        ColumnDescription c1 = new ColumnDescription("Age", ContentsKind.Integer, false);
        CategoryArrayColumn sac = new CategoryArrayColumn(c0,
                new String[] { "Mike", "John", "Tom", "Bill", "Bill", "Smith", "Donald", "Bruce",
                               "Bob", "Frank", "Richard", "Steve", "Dave" });
        IntArrayColumn iac = new IntArrayColumn(c1, new int[] { 20, 30, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        return new Table(Arrays.asList(sac, iac));
    }

    /**
     * Can be used for testing.
     * @return A small table with some repeated content.
     */
    public static Table testRepTable() {
        ColumnDescription c0 = new ColumnDescription("Name", ContentsKind.Category, false);
        ColumnDescription c1 = new ColumnDescription("Age", ContentsKind.Integer, false);
        CategoryArrayColumn sac = new CategoryArrayColumn(c0,
                new String[] { "Mike", "John", "Tom", "Bill", "Bill", "Smith", "Donald", "Bruce",
                        "Bob", "Frank", "Richard", "Steve", "Dave", "Mike", "Ed" });
        IntArrayColumn iac = new IntArrayColumn(c1, new int[] { 20, 30, 10, 10, 20, 30, 20, 30, 10,
                40, 40, 20, 10, 50, 60 });
        return new Table(Arrays.asList(sac, iac));
    }

    /**
     * Can be used for testing large tables with strings.
     * @param size Number of rows in the table
     * @param others Array of options in the "Name" column
     * @param count Number of occurrences of the 'test' string.
     * @param test The string that should occur 'count' times.
     * @return A table with an arbitrary number of rows. It contains 'count' rows that have 'test' in the Name column.
     */
    public static Table testLargeStringTable(int size, String[] others, int count, String test) {
        ColumnDescription c0 = new ColumnDescription("Name", ContentsKind.Category, false);
        ColumnDescription c1 = new ColumnDescription("Age", ContentsKind.Integer, false);

        Assert.assertTrue(!Arrays.asList(others).contains(test));
        Random random = new Random();
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> ages = new ArrayList<Integer>();
        for (int i = 0; i < size - count; i++) {
            String name = others[random.nextInt(others.length)];
            names.add(name);
            ages.add(random.nextInt(60) + 20);
        }

        // Add 'count' test names.
        for (int i = 0; i < count; i++) {
            names.add(test);
            ages.add(random.nextInt(60) + 20);
        }

        // Shuffle the lists, just to be sure.
        long seed = System.nanoTime();
        Collections.shuffle(names, new Random(seed));
        Collections.shuffle(ages, new Random(seed));

        StringArrayColumn sac = new StringArrayColumn(c0, names.toArray(new String[0]));
        IntArrayColumn iac = new IntArrayColumn(c1, ages.stream().mapToInt(i -> i).toArray());

        return new Table(Arrays.asList(sac, iac));
    }

    /**
     * A table of integers whose rows are typically distinct. Each row is sampled randomly from a
     * domain of size 5^numCols*size. When numCols is small, some collisions are to be expected, but
     * generally the elements are distinct (each row in the range has a probability of 5^{-numCols}
     * of being sampled.)
     * @param size The size of the desired table
     * @param numCols The number of columns
     * @return A table of random integers.
     */
    public static SmallTable getIntTable(final int size, final int numCols) {
        Randomness rn = new Randomness(2); // we want deterministic random numbers for testing
        final List<IColumn> columns = new ArrayList<IColumn>(numCols);
        double exp = 1.0/numCols;
        final int range =  5*((int)Math.pow(size, exp));
        for (int i = 0; i < numCols; i++) {
            final String colName = "Column" + String.valueOf(i);
            columns.add(IntArrayGenerator.getRandIntArray(size, range, colName, rn));
        }
        return new SmallTable(columns);
    }

    /**
     * A table of integers with some missing values. Each column is the just the identity, but with
     * every multiple of some integer mo in {0,..,99} missing.
     * @param size The size of the desired table
     * @param numCols The number of columns
     * @return A table of integers with missing values.
     */
    public static SmallTable getMissingIntTable(final int size, final int numCols) {
        Randomness rn = new Randomness(2); // we want deterministic random numbers for testing
        final List<IColumn> columns = new ArrayList<IColumn>(numCols);
        double exp = 1.0/numCols;
        final int range =  5*((int)Math.pow(size, exp));
        for (int i = 0; i < numCols; i++) {
            int mod = rn.nextInt(9) + 1;
            final String colName = "Missing" + String.valueOf(mod);
            columns.add(IntArrayGenerator.getMissingIntArray(colName, size, mod));
        }
        return new SmallTable(columns);
    }
    /**
     * A table of integers where each row typically occurs multiple times. Each row is sampled
     * randomly from a domain of size size^{4/5}.  Collisions are to be expected, each tuple from
     * the range appears with frequency size^{1/5} in expectation.
     * @param size The size of the desired table
     * @param numCols The number of columns
     * @return A table of integers with repeated rows.
     */
    public static Table getRepIntTable(final int size, final int numCols) {
        Randomness rn = new Randomness(1); // we want deterministic random numbers for testing
        final List<IColumn> columns = new ArrayList<IColumn>(numCols);
        double exp = 0.8 / numCols;
        final int range =  ((int)Math.pow(size, exp));
        for (int i = 0; i < numCols; i++) {
            final String colName = "Column" + String.valueOf(i);
            columns.add(IntArrayGenerator.getRandIntArray(size, range, colName, rn));
        }
        final FullMembership full = new FullMembership(size);
        return new Table(columns, full);
    }

    /**
     * Method generates a table with a specified number of integer columns, where each column is
     * generated by the GetHeavyIntTable Method so the frequencies are geometrically increasing
     * @param numCols number of columns
     * @param size rows per column
     * @param base base parameter for GetHeavyIntTable
     * @param range range parameter for GetHeavyIntTable
     * @return A table of integers.
     */
    public static SmallTable getHeavyIntTable(final int numCols, final int size, final double base,
                                              final int range) {
        Randomness rn = new Randomness(3);
        final List<IColumn> columns = new ArrayList<IColumn>(numCols);
        for (int i = 0; i < numCols; i++) {
            final String colName = "Column" + String.valueOf(i);
            columns.add(IntArrayGenerator.getHeavyIntArray(size, base, range, colName, rn));
        }
        return new SmallTable(columns);
    }

    /**
     * Generates a table with a specified number of correlated columns. Each row has the same
     * absolute value in every column, they only differ in the sign (which is drawn randomly).
     * - Column 0 contains non-negative integers drawn at random from (0, range).
     * - The signs in the i^th column are  controlled by a parameter rho[i] in (0,1) which is
     * drawn at random. The sign of the i^th column is +1 with probability rho[i] and -1 with
     * probability (1 - rho[i]) independently for every row.
     * - The normalized correlation between Column 0 and Column i is 2*rho[i] - 1 in [-1,1], in
     * expectation.
     * @param size The number of rows.
     * @param numCols The number of columns.
     * @param range Each entry lies in {0, ..., range} in absolute value.
     * @return A table with correlated integer columns.
     */
    public static SmallTable getCorrelatedCols(final int size, final int numCols, final int range) {
        Randomness rn = new Randomness(100); // predictable randomness for testing
        double[] rho = new double[numCols];
        ColumnDescription[] desc = new ColumnDescription[numCols];
        String[] name = new String[numCols];
        IntArrayColumn[] intCol = new IntArrayColumn[numCols];
        for (int i =0; i<  numCols; i++) {
            name[i] = "Col" + String.valueOf(i);
            desc[i] = new ColumnDescription(name[i], ContentsKind.Integer, false);
            intCol[i] = new IntArrayColumn(desc[i], size);
            rho[i] = ((i==0) ? 1 : (rho[i-1]*0.8));
            //System.out.printf("Rho %d = %f\n",i, rho[i]);
        }
        for (int i = 0; i < size; i++) {
            int k = rn.nextInt(range);
            for (int j = 0; j < numCols; j++) {
                double x = rn.nextDouble();
                intCol[j].set(i, ((x > rho[j]) ? -k: k));
            }
        }
        final List<IColumn> col = new ArrayList<IColumn>();
        col.addAll(Arrays.asList(intCol).subList(0, numCols));
        return new SmallTable(col);
    }

    /**
     * @param size Number of rows in the table.
     * @param numCols Number of columns in the table. Has to be >= 2.
     * @return A table where the 2nd column is a linear function of the 1st (with some noise). The rest of the columns
     * contains just small noise from a Gaussian distribution.
     */
    public static ITable getLinearTable(final int size, final int numCols) {
        Random rnd = new Random(42);
        double noise = 0.01;
        double a = 1.2;

        DoubleMatrix mat = new DoubleMatrix(size, numCols);
        for (int i = 0; i < size; i++) {
            // Make the first two columns linearly correlated.
            double x = rnd.nextDouble();
            mat.put(i, 0, x);
            double y = a * x + rnd.nextGaussian() * noise;
            mat.put(i, 1, y);

            // Fill the rest of the columns with noise.
            for (int j = 2; j < numCols; j++) {
                double z = noise * rnd.nextGaussian();
                mat.put(i, j, z);
            }
        }

        return BlasConversions.toTable(mat);
    }

    public static ITable getCentroidTestTable() {
        DoubleArrayColumn colX = new DoubleArrayColumn(
                new ColumnDescription("x", ContentsKind.Double, false),
                new double[]{1, 2, 2, 3, 4, 5, 5, 6}
        );
        DoubleArrayColumn colY = new DoubleArrayColumn(
                new ColumnDescription("y", ContentsKind.Double, false),
                new double[]{11, 10, 12, 11, 26, 25, 27, 26}
        );
        CategoryArrayColumn fruitType = new CategoryArrayColumn(
                new ColumnDescription("FruitType", ContentsKind.Category, false),
                new String[]{"Banana", "Banana", "Banana", "Banana", "Orange", "Orange", "Orange", "Orange"}
        );
        return new Table(Arrays.asList(colX, colY, fruitType));
    }

    /**
     * Splits a Big Table into a list of Small Tables.
     * @param bigTable The big table
     * @param fragmentSize The size of each small Table
     * @return A list of small tables of size at most fragment size.
     */
    public static List<ITable> splitTable(ITable bigTable, int fragmentSize) {
        int tableSize = bigTable.getNumOfRows();
        int numTables = (tableSize / fragmentSize) + 1;
        List<ITable> tableList = new ArrayList<ITable>(numTables);
        int start = 0;
        while (start < tableSize) {
            int thisFragSize = Math.min(fragmentSize, tableSize - start);
            IMembershipSet members = new SparseMembership(start, thisFragSize, tableSize);
            tableList.add(bigTable.selectRowsFromFullTable(members));
            start += fragmentSize;
        }
        return tableList;
    }

    /**
     * Creates a ParallelDataSet from a Big Table
     * @param bigTable The big table
     * @param fragmentSize The size of each small Table
     * @return A Parallel Data Set containing the data in the Big Table.
     */
    public static ParallelDataSet<ITable> makeParallel(ITable bigTable, int fragmentSize) {
        final List<ITable> tabList = splitTable(bigTable, fragmentSize);
        final ArrayList<IDataSet<ITable>> a = new ArrayList<IDataSet<ITable>>();
        for (ITable t : tabList) {
            LocalDataSet<ITable> ds = new LocalDataSet<ITable>(t);
            a.add(ds);
        }
        return new ParallelDataSet<ITable>(a);
    }
}
