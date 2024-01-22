package org.savedoc.cdc4j.common;

import java.util.List;

public final class TableMetadata {
    private final String tableName;
    private final String tableSchema;
    private final List<Column> columns;

    public TableMetadata(String tableName, String tableSchema, List<Column> columns) {
        this.tableName = tableName;
        this.tableSchema = tableSchema;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public List<Column> getColumns() {
        return columns;
    }
}
