package org.savedoc.cdc4j.postgresql.handlers;

import org.savedoc.cdc4j.common.CdcEvent;
import org.savedoc.cdc4j.common.TableMetadata;

import java.nio.ByteBuffer;
import java.util.Map;

public interface Handler {

    char getType();

    CdcEvent handle(ByteBuffer byteBuffer, Map<String, TableMetadata> tableMetadata);
}
