package org.savedoc.cdc4j.common;

import static org.savedoc.cdc4j.common.ValidationUtils.isEmpty;

public final class PublicationTable {
    private final String tableSchema;
    private final String tableName;

    private PublicationTable(String tableSchema, String tableName) {
        this.tableSchema = tableSchema;
        this.tableName = tableName;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String tableSchema;
        private String tableName;

        public Builder withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder withTableSchema(String tableSchema) {
            this.tableSchema = tableSchema;
            return this;
        }

        public PublicationTable build() {
            if (isEmpty(tableSchema)) {
                throw new ClientException("Table schema is not set. Please use PublicationTable.builder().withTableSchema(\"tableSchema\")...");
            }
            if (isEmpty(tableName)) {
                throw new ClientException("Table name is not set. Please use PublicationTable.builder().withTableName(\"tableName\")...");
            }
            return new PublicationTable(tableSchema, tableName);
        }
    }
}
