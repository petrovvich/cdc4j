package org.savedoc.cdc4j.common;

public final class TupleData {
    private final Short numberOfColumns;
    private final char dataType;
    private final int columnLength;
    private final String columnValue;

    public TupleData(Short numberOfColumns, char dataType, int columnLength, String columnValue) {
        this.numberOfColumns = numberOfColumns;
        this.dataType = dataType;
        this.columnLength = columnLength;
        this.columnValue = columnValue;
    }

    public Short getNumberOfColumns() {
        return numberOfColumns;
    }

    public char getDataType() {
        return dataType;
    }

    public int getColumnLength() {
        return columnLength;
    }

    public String getColumnValue() {
        return columnValue;
    }
}
