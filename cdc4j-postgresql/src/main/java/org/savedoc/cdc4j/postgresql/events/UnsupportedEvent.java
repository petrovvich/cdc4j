package org.savedoc.cdc4j.postgresql.events;

import org.savedoc.cdc4j.common.CdcEvent;
import org.savedoc.cdc4j.common.CdcEventType;

import java.util.Collections;
import java.util.Map;

public final class UnsupportedEvent implements CdcEvent {
    @Override
    public CdcEventType getType() {
        return CdcEventType.UNSUPPORTED;
    }

    @Override
    public String getRelationOId() {
        return "";
    }

    @Override
    public String getTableName() {
        return "";
    }

    @Override
    public String getTableSchema() {
        return "";
    }

    @Override
    public Map<String, String> getColumns() {
        return Collections.emptyMap();
    }
}
