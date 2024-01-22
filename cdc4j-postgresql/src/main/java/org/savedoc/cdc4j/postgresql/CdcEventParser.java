package org.savedoc.cdc4j.postgresql;

import org.savedoc.cdc4j.common.CdcEvent;
import org.savedoc.cdc4j.common.TableMetadata;
import org.savedoc.cdc4j.postgresql.events.UnsupportedEvent;
import org.savedoc.cdc4j.postgresql.handlers.DeleteHandler;
import org.savedoc.cdc4j.postgresql.handlers.Handler;
import org.savedoc.cdc4j.postgresql.handlers.InsertHandler;
import org.savedoc.cdc4j.postgresql.handlers.UpdateHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see <a href="https://www.postgresql.org/docs/current/protocol-logicalrep-message-formats.html">Official POstgreSQL documentation</a>
 */
public final class CdcEventParser {
    private final Map<Character, Handler> handlers = Stream
            .of(new InsertHandler(), new UpdateHandler(), new DeleteHandler())
            .collect(Collectors.toMap(Handler::getType, Function.identity()));

    public CdcEvent parse(ByteBuffer binaryData, Map<String, TableMetadata> tableMetadata) {
        final var leadingLetter = (char) binaryData.get(0);
        return Optional.ofNullable(handlers.get(leadingLetter))
                .map(handler -> handler.handle(binaryData, tableMetadata))
                .orElse(new UnsupportedEvent());
    }
}
