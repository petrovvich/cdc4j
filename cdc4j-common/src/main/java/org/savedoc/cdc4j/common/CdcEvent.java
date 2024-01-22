package org.savedoc.cdc4j.common;

import java.util.Map;

public interface CdcEvent {
    CdcEventType getType();

    String getRelationOId();

    String getTableName();

    String getTableSchema();

    Map<String, String> getColumns();
}
