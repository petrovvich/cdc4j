package org.savedoc.cdc4j.postgresql.events;

import org.savedoc.cdc4j.common.CdcEvent;
import org.savedoc.cdc4j.common.CdcEventType;

import java.util.Map;
import java.util.StringJoiner;

public class CdcEventSimple implements CdcEvent {
    private final CdcEventType type;
    private final String relationOId;
    private final String tableName;
    private final String tableSchema;
    private final Map<String, String> columns;

    public CdcEventSimple(CdcEventType type, String relationOId, String tableName, String tableSchema,
                          Map<String, String> columns) {
        this.type = type;
        this.relationOId = relationOId;
        this.tableName = tableName;
        this.tableSchema = tableSchema;
        this.columns = columns;
    }

    @Override
    public CdcEventType getType() {
        return type;
    }

    @Override
    public String getRelationOId() {
        return relationOId;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getTableSchema() {
        return tableSchema;
    }

    @Override
    public Map<String, String> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CdcEventSimple.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("relationOId='" + relationOId + "'")
                .add("tableName='" + tableName + "'")
                .add("tableSchema='" + tableSchema + "'")
                .add("columns=" + columns)
                .toString();
    }
}
