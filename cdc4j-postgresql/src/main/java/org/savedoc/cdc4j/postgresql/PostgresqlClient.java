package org.savedoc.cdc4j.postgresql;

import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.replication.PGReplicationStream;
import org.savedoc.cdc4j.common.*;
import org.savedoc.cdc4j.postgresql.events.CdcEventSimple;
import org.savedoc.cdc4j.postgresql.events.EventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.savedoc.cdc4j.common.ValidationUtils.isEmpty;
import static org.savedoc.cdc4j.common.ValidationUtils.isNotAlphaNumeric;

public final class PostgresqlClient implements CdcClient {
    private static final Optional<CdcEvent> EMPTY_RESPONSE = Optional.empty();
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WRONG_WAL_LEVEL_ERROR = "Required wal level for CDC is logical. Please update configuration by running following command ALTER SYSTEM SET wal_level = logical; and then restart database";
    private static final String DEFAULT_PLUGIN_NAME = "pgoutput";
    private static final String REQUIRED_WAL_LEVEL = "logical";
    private static final String PUBLICATION_NAME = "cdc4j";
    private final CdcEventParser cdcEventParser = new CdcEventParser();
    private final Connection connection;
    private final String replicationSlotName;
    private final String pluginName;
    private List<PublicationTable> tables;
    private final Set<CdcEventType> eventTypes;
    private final Map<String, TableMetadata> tableMetadata = new HashMap<>();
    private PGReplicationStream pgReplicationStream = null;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentLinkedQueue<EventWrapper> messageQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private PostgresqlClient(Connection connection,
                             Set<CdcEventType> cdcEventTypes,
                             String replicationSlotName,
                             String pluginName,
                             List<PublicationTable> tables) {
        this.connection = connection;
        this.eventTypes = cdcEventTypes;
        this.replicationSlotName = replicationSlotName;
        this.pluginName = pluginName;
        this.tables = tables;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void start() {
        checkReplicationMode();
        createOrGetPublication();
        createOrGetReplicationSlot();
        addTablesToReplicationSlot();
        configureTablesForReplication();
        collectTablesMetadata();
        startReplicationSlot();
        startConsumingEvents();
    }

    @Override
    public Optional<CdcEvent> read() {
        try {
            final var event = messageQueue.peek();
            if (event != null) {
                pgReplicationStream.setAppliedLSN(event.getLastLSN());
                pgReplicationStream.setFlushedLSN(event.getLastLSN());
                return Optional.of(messageQueue.poll().getEvent());
            }
        } catch (Exception e) {
            log.error("Can not read events from connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }

        return EMPTY_RESPONSE;
    }

    private void checkReplicationMode() {
        try (final var stmt = connection.createStatement()) {
            final var resultSet = stmt.executeQuery("show wal_level");
            if (resultSet.next()) {
                final var walLevel = resultSet.getString(1);
                if (!REQUIRED_WAL_LEVEL.equalsIgnoreCase(walLevel)) {
                    throw new ConfigurationException(WRONG_WAL_LEVEL_ERROR);
                }
            }
        } catch (Exception e) {
            closeConnection();
            log.error("Can not initialize connection", e);
            throw new RuntimeException(e);
        }
    }

    private void createOrGetPublication() {
        try (final var stmt = connection.prepareStatement("select * from pg_publication where pubname = ?")) {
            stmt.setString(1, PUBLICATION_NAME);
            final var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                if (log.isDebugEnabled()) {
                    log.debug("Publication {} exists, skip initialization", PUBLICATION_NAME);
                }
            } else {
                final var stmtCreate = connection.prepareStatement("create publication " + PUBLICATION_NAME);
                final var createResult = stmtCreate.executeQuery();
                if (createResult.next()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Publication {} created", replicationSlotName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Can not initialize connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void createOrGetReplicationSlot() {
        try (final var stmt = connection.prepareStatement("select * from pg_replication_slots where slot_name = ?")) {
            stmt.setString(1, replicationSlotName);
            final var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                if (log.isDebugEnabled()) {
                    log.debug("Replication slot {} exists, skip initialization", replicationSlotName);
                }
            } else {
                final var stmtCreateSlot = connection.prepareStatement("select pg_create_logical_replication_slot(?, ?)");
                stmtCreateSlot.setString(1, replicationSlotName);
                stmtCreateSlot.setString(2, pluginName);
                final var createResult = stmtCreateSlot.executeQuery();
                if (createResult.next()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Replication slot {} created", replicationSlotName);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Can not initialize connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void addTablesToReplicationSlot() {
        try {
            for (PublicationTable rt : tables) {
                final var readConfigStmt = connection.prepareStatement("select * from pg_publication_tables where pubname = ? and tablename = ? and schemaname = ?");
                readConfigStmt.setString(1, PUBLICATION_NAME);
                readConfigStmt.setString(2, rt.getTableName());
                readConfigStmt.setString(3, rt.getTableSchema());
                final var configResultSet = readConfigStmt.executeQuery();
                if (configResultSet.next()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Table {}.{} already added to replication", rt.getTableSchema(), rt.getTableName());
                    }
                } else {
                    final var addConfigStmt = connection.prepareStatement("ALTER PUBLICATION " + PUBLICATION_NAME + " ADD TABLE " + rt.getTableName());
                    addConfigStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            log.error("Can not initialize connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void configureTablesForReplication() {
        Arrays.stream(CdcEventType.values())
                .filter(eventType -> CdcEventType.UNSUPPORTED != eventType)
                .collect(Collectors.toList())
                .forEach(eventType -> {
                    if (eventTypes.contains(eventType)) {
                        if (eventType == CdcEventType.DELETE) {
                            updateReplicaIdentityForAllTables();
                        }
                        try (final var stmt = connection.prepareStatement("update pg_publication set " + eventType.getSqlName() + " = true where pubname = ?")) {
                            stmt.setString(1, PUBLICATION_NAME);
                            stmt.executeUpdate();
                        } catch (Exception exception) {
                            log.error("Can not initialize connection", exception);
                        }
                    } else {
                        try (final var stmt = connection.prepareStatement("update pg_publication set " + eventType.getSqlName() + " = false where pubname = ?")) {
                            stmt.setString(1, PUBLICATION_NAME);
                            stmt.executeUpdate();
                        } catch (Exception exception) {
                            log.error("Can not initialize connection", exception);
                        }
                    }
                });
    }

    private void updateReplicaIdentityForAllTables() {
        for (PublicationTable pt : tables) {
            try (final var stmt = connection.prepareStatement("ALTER TABLE " + pt.getTableSchema() + "." + pt.getTableName() + " REPLICA IDENTITY full")) {
                stmt.executeQuery();
            } catch (Exception exception) {
                log.error("Can not update replica identity", exception);
            }
        }
    }

    private void collectTablesMetadata() {
        try {
            for (PublicationTable table : tables) {
                final var oIdStmt = connection.prepareStatement("SELECT oid FROM pg_class WHERE relname = ? AND relkind = 'r'");
                oIdStmt.setString(1, table.getTableName());
                final var oIdRs = oIdStmt.executeQuery();
                var tableOId = "-1";
                if (oIdRs.next()) {
                    tableOId = oIdRs.getString(1);
                }
                final var columnsStmt = connection.prepareStatement("select column_name, data_type, ordinal_position from INFORMATION_SCHEMA.COLUMNS where table_name = ? and table_schema = ?");
                columnsStmt.setString(1, table.getTableName());
                columnsStmt.setString(2, table.getTableSchema());
                final var columnsRs = columnsStmt.executeQuery();
                final var columns = new ArrayList<Column>();
                while (columnsRs.next()) {
                    columns.add(new Column(tableOId, columnsRs.getString(1), columnsRs.getString(2), columnsRs.getInt(3)));
                }
                tableMetadata.put(tableOId, new TableMetadata(table.getTableName(), table.getTableSchema(), columns));
            }
        } catch (Exception e) {
            log.error("Can not initialize connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void startReplicationSlot() {
        try {
            pgReplicationStream = connection.unwrap(PGConnection.class)
                    .getReplicationAPI()
                    .replicationStream()
                    .logical()
                    .withSlotName(replicationSlotName)
                    .withStatusInterval(500, TimeUnit.MILLISECONDS)
                    .withSlotOption("proto_version", "1")
                    .withSlotOption("publication_names", PUBLICATION_NAME)
                    .start();
        } catch (Exception e) {
            log.error("Can not initialize connection", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void startConsumingEvents() {
        executorService
                .scheduleAtFixedRate(
                        () -> {
                            while (running.get()) {
                                try {
                                    final var pending = pgReplicationStream.readPending();
                                    if (pending != null) {
                                        final var event = cdcEventParser.parse(pending, tableMetadata);
                                        if (event instanceof CdcEventSimple) {
                                            messageQueue.add(new EventWrapper(pgReplicationStream.getLastReceiveLSN(), event));
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Can not read event", e);
                                }

                            }
                        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public static final class Builder {
        private String username;
        private String password;
        private String host;
        private String port;
        private String databaseName;
        private String connectionParams;
        private String replicationSlotName;
        private String pluginName;
        private List<PublicationTable> publicationTables;
        private Set<CdcEventType> eventTypes;

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(String port) {
            this.port = port;
            return this;
        }

        public Builder withDatabaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder withConnectionParams(String connectionParams) {
            this.connectionParams = connectionParams;
            return this;
        }

        public Builder withReplicationSlotName(final String replicationSlotName) {
            this.replicationSlotName = replicationSlotName;
            return this;
        }

        public Builder withPluginName(final String pluginName) {
            this.pluginName = pluginName;
            return this;
        }

        public Builder withTables(List<PublicationTable> publicationTables) {
            this.publicationTables = publicationTables;
            return this;
        }

        public Builder withEvents(Set<CdcEventType> events) {
            this.eventTypes = events;
            return this;
        }

        public PostgresqlClient build() {
            // FIXME: Move to separate validator
            if (ValidationUtils.isEmpty(username)) {
                throw new ClientException("Datasource username is not set. Please use PostgresqlClient.builder().withUsername(\"username\")...");
            }
            if (ValidationUtils.isEmpty(password)) {
                throw new ClientException("Datasource password is not set. Please use PostgresqlClient.builder().withPassword(\"password\")...");
            }
            if (ValidationUtils.isEmpty(host)) {
                throw new ClientException("Datasource host is not set. Please use PostgresqlClient.builder().withHost(\"host\")...");
            }
            if (ValidationUtils.isEmpty(port)) {
                throw new ClientException("Datasource port is not set. Please use PostgresqlClient.builder().withPort(\"port\")...");
            }
            if (ValidationUtils.isNotNumeric(port)) {
                throw new ClientException("Datasource port is not a number. Please use PostgresqlClient.builder().withPort(\"5432\")...");
            }
            if (ValidationUtils.isEmpty(databaseName)) {
                throw new ClientException("Datasource database name is not set. Please use PostgresqlClient.builder().withDatabaseName(\"databaseName\")...");
            }
            if (isEmpty(replicationSlotName)) {
                throw new ClientException("Replication slot name is not set. Please use PostgresqlClient.builder().withName(\"name\")...");
            }
            if (isNotAlphaNumeric(replicationSlotName)) {
                throw new ClientException("Replication slot name " + replicationSlotName + " contains invalid character. Replication slot names may only contain lower case letters, numbers, and the underscore character");
            }
            try {
                final var props = new Properties();
                PGProperty.USER.set(props, username);
                PGProperty.PASSWORD.set(props, password);
                PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "10");
                PGProperty.REPLICATION.set(props, "database");
                PGProperty.PREFER_QUERY_MODE.set(props, "simple");
                final var connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + databaseName
                        + (ValidationUtils.isEmpty(connectionParams) ? "" : "?" + connectionParams), props);
                return new PostgresqlClient(
                        connection,
                        Optional.ofNullable(eventTypes).orElse(Stream.of(CdcEventType.values()).collect(Collectors.toSet())),
                        replicationSlotName,
                        Optional.ofNullable(pluginName).orElse(DEFAULT_PLUGIN_NAME),
                        Optional.ofNullable(publicationTables).orElse(new ArrayList<>()));
            } catch (SQLException ec) {
                throw new RuntimeException(ec);
            }

        }
    }

    private void closeConnection() {
        try {
            running.set(false);
            if (pgReplicationStream != null && !pgReplicationStream.isClosed()) {
                pgReplicationStream.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
