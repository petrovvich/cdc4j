package org.savedoc.cdc4j.common;

public final class Column {
    private final String tableOId;
    private final String name;
    private final String dataType;
    private final int position;

    public Column(String tableOId, String name, String dataType, int position) {
        this.tableOId = tableOId;
        this.name = name;
        this.dataType = dataType;
        this.position = position;
    }

    public String getTableOId() {
        return tableOId;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public int getPosition() {
        return position;
    }
}
