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
 */

package org.hillview.storage;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.NanoTime;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.hillview.table.ColumnDescription;
import org.hillview.table.Table;
import org.hillview.table.api.*;
import org.hillview.table.columns.BaseListColumn;
import org.hillview.utils.Converters;
import org.hillview.utils.Linq;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ParquetFileLoader extends TextFileLoader {
    private final boolean lazy;
    private final Path path;
    private final Configuration configuration;
    private final ParquetMetadata metadata;

    public ParquetFileLoader(String filename, boolean lazy) {
        super(filename);
        this.path = new Path(this.filename);
        this.lazy = lazy;
        this.configuration = new Configuration();
        System.setProperty("hadoop.home.dir", "/");
        this.configuration.set("hadoop.security.authentication", "simple");
        this.configuration.set("hadoop.security.authorization", "false");
        try {
            ParquetReadOptions.Builder builder = new ParquetReadOptions.Builder();
            InputFile file = HadoopInputFile.fromPath(path, this.configuration);
            ParquetFileReader parquetFileReader = new ParquetFileReader(file, builder.build());
            this.metadata = parquetFileReader.getFooter();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("RedundantCast")
    private static void appendGroup(
            List<IAppendableColumn> cols, Group g, List<ColumnDescriptor> cds) {
        int fieldCount = g.getType().getFieldCount();
        for (int field = 0; field < fieldCount; field++) {
            int valueCount = g.getFieldRepetitionCount(field);
            IAppendableColumn col = cols.get(field);
            if (valueCount == 0) {
                col.appendMissing();
                continue;
            }
            if (valueCount > 1)
                throw new RuntimeException("Repeated values not supported");

            Type fieldType = g.getType().getType(field);
            if (!fieldType.isPrimitive())
                throw new RuntimeException("Non-primitive field not supported");
            switch (cds.get(field).getPrimitiveType().getPrimitiveTypeName()) {
                case INT64: {
                    long l = g.getLong(field, 0);
                    col.append((double) l);
                    break;
                }
                case FLOAT: {
                    float f = g.getFloat(field, 0);
                    col.append((double) f);
                    break;
                }
                case DOUBLE: {
                    double d = g.getDouble(field, 0);
                    col.append(d);
                    break;
                }
                case INT32: {
                    int i = g.getInteger(field, 0);
                    col.append(i);
                    break;
                }
                case BOOLEAN: {
                    boolean b = g.getBoolean(field, 0);
                    col.append(b ? "true" : "false");
                    break;
                }
                case BINARY: {
                    Binary b = g.getBinary(field, 0);
                    String s = b.toStringUsingUTF8();
                    col.append(s);
                    break;
                }
                case FIXED_LEN_BYTE_ARRAY: {
                    String s = g.getString(field, 0);
                    col.append(s);
                    break;
                }
                case INT96: {
                    // We are assuming that this is a Hive/Impala timestamp
                    // from the drill ParquetReaderUtility.java file
                    final long JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH = 2440588;
                    Binary val = g.getInt96(field, 0);
                    NanoTime nt = NanoTime.fromBinary(val);
                    int julianDay = nt.getJulianDay();
                    long nanosOfDay = nt.getTimeOfDayNanos();
                    long epochSeconds = (julianDay - JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH) * Converters.SECONDS_TO_DAY + nanosOfDay / Converters.NANOS_TO_SECONDS;
                    LocalDateTime inst = LocalDateTime.ofEpochSecond(epochSeconds, Converters.toInt(nanosOfDay % Converters.NANOS_TO_SECONDS), ZoneOffset.UTC);
                    col.append(Converters.toDouble(inst));
                    break;
                }
                default:
                    throw new RuntimeException("Unexpected column kind " + cds.get(field).getPrimitiveType().getPrimitiveTypeName());
            }
        }
    }

    private static ColumnDescription getColumnDescription(ColumnDescriptor cd) {
        String name = String.join("", cd.getPath());  // this should contain a single String
        ContentsKind kind;
        switch (cd.getPrimitiveType().getPrimitiveTypeName()) {
            case INT64:
            case FLOAT:
            case DOUBLE:
                kind = ContentsKind.Double;
                break;
            case INT32:
                kind = ContentsKind.Integer;
                break;
            case BOOLEAN:
            case BINARY:
            case FIXED_LEN_BYTE_ARRAY:
                kind = ContentsKind.String;
                break;
            case INT96:
                kind = ContentsKind.LocalDate;
                break;
            default:
                throw new RuntimeException("Unexpected column kind " + cd.getPrimitiveType());
        }
        return new ColumnDescription(name, kind);
    }

    private static List<IAppendableColumn> createColumns(ParquetMetadata md) {
        MessageType schema = md.getFileMetaData().getSchema();
        List<ColumnDescriptor> cols = schema.getColumns();
        List<IAppendableColumn> result = new ArrayList<IAppendableColumn>(cols.size());

        for (ColumnDescriptor cd : cols) {
            ColumnDescription desc = getColumnDescription(cd);
            result.add(BaseListColumn.create(desc));
        }
        return result;
    }

    public class ParquetColumnLoader implements IColumnLoader {
        @Override
        public List<? extends IColumn> loadColumns(List<String> names) {
            FileMetaData fm = ParquetFileLoader.this.metadata.getFileMetaData();
            MessageType schema = fm.getSchema();
            List<Type> list = new ArrayList<Type>();
            for (Type col : schema.getFields()) {
                String colName = col.getName();
                if (names.contains(colName))
                    list.add(col);
            }
            assert list.size() > 0;
            MessageType newSchema = new MessageType(schema.getName(), list);
            FileMetaData nfm = new FileMetaData(
                    newSchema, fm.getKeyValueMetaData(), fm.getCreatedBy());
            List<BlockMetaData> blocks = ParquetFileLoader.this.metadata.getBlocks();
            ParquetMetadata md = new ParquetMetadata(nfm, blocks);
            return ParquetFileLoader.this.loadColumns(md);
        }
    }

    private List<IColumn> loadColumns(ParquetMetadata md) {
        try {
            MessageType schema = md.getFileMetaData().getSchema();
            List<IAppendableColumn> cols = createColumns(md);
            ParquetReadOptions.Builder builder = new ParquetReadOptions.Builder();
            InputFile file = HadoopInputFile.fromPath(path, this.configuration);
            ParquetFileReader r = new ParquetFileReader(file, builder.build());
            MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);

            PageReadStore pages;
            while (null != (pages = r.readNextRowGroup())) {
                final long rows = pages.getRowCount();
                RecordReader<Group> recordReader = columnIO.getRecordReader(
                        pages, new GroupRecordConverter(schema));
                for (int i = 0; i < rows; i++) {
                    Group g = recordReader.read();
                    appendGroup(cols, g, md.getFileMetaData().getSchema().getColumns());
                }
            }

            for (IAppendableColumn c : cols)
                c.seal();
            r.close();
            return Linq.map(cols, e -> e);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int getNumRows() {
        List<BlockMetaData> blocks = this.metadata.getBlocks();
        long rowCount = 0;
        for (BlockMetaData bm : blocks)
            rowCount += bm.getRowCount();
        return Converters.toInt(rowCount);
    }

    public ITable load() {
        ParquetMetadata md = this.metadata;
        if (this.lazy) {
            ParquetColumnLoader loader = new ParquetColumnLoader();
            List<ColumnDescriptor> cds = md.getFileMetaData().getSchema().getColumns();
            int size = this.getNumRows();
            List<ColumnDescription> desc = Linq.map(cds,
                    ParquetFileLoader::getColumnDescription);
            Table result = Table.createLazyTable(desc, size, this.filename, loader);
            this.close(null);
            return result;
        } else {
            List<IColumn> cols = this.loadColumns(md);
            this.close(null);
            return new Table(cols, this.filename, null);
        }
    }
}
