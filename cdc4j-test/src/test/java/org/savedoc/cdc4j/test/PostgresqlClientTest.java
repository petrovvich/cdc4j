package org.savedoc.cdc4j.test;

import org.junit.jupiter.api.Test;
import org.savedoc.cdc4j.common.CdcEventType;
import org.savedoc.cdc4j.common.PublicationTable;
import org.savedoc.cdc4j.postgresql.PostgresqlClient;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PostgresqlClientTest extends IntegrationTest {

    @Test
    void testCdc_shouldThrowExceptionInvalidWalType() {
        final var db = buildPostgres("postgres:16.1-alpine", "");
        final var client = PostgresqlClient.builder()
                .withUsername(db.getUsername())
                .withPassword(db.getPassword())
                .withHost(db.getHost())
                .withPort(db.getFirstMappedPort().toString())
                .withDatabaseName(db.getDatabaseName())
                .withReplicationSlotName("test_cdc")
                .withTables(List.of(PublicationTable.builder()
                        .withTableSchema("public")
                        .withTableName("users")
                        .build()))
                .withEvents(Set.of(CdcEventType.INSERT))
                .build();
        assertAll(
                () -> assertNotNull(client),
                () -> assertThrows(RuntimeException.class, client::start)
        );
    }
}
