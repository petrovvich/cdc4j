package org.savedoc.cdc4j.postgresql.handlers;

import org.savedoc.cdc4j.common.CdcEvent;
import org.savedoc.cdc4j.common.CdcEventType;
import org.savedoc.cdc4j.common.TableMetadata;
import org.savedoc.cdc4j.postgresql.events.CdcEventSimple;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.savedoc.cdc4j.common.EventUtils.readTuple;

public class DeleteHandler implements Handler {
    @Override
    public char getType() {
        return 'D';
    }

    @Override
    public CdcEvent handle(ByteBuffer byteBuffer, Map<String, TableMetadata> tableMetadata) {
        var offset = 1;
        final var relationOId = byteBuffer.getInt(offset);
        offset += Integer.BYTES;
        final var newMessage = (char) byteBuffer.get(offset);
        ++offset;
        final var tupleData = readTuple(byteBuffer, offset);
        final var columns = new HashMap<String, String>();
        final var table = tableMetadata.get(relationOId + "");
        for (int i = 0; i < tupleData.size(); i++) {
            columns.put(table.getColumns().get(i).getName(), tupleData.get(i).getColumnValue());
        }
        return new CdcEventSimple(CdcEventType.DELETE, relationOId + "", table.getTableName(), table.getTableSchema(), columns);
    }
}
