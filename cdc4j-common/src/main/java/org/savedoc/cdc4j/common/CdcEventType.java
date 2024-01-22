package org.savedoc.cdc4j.common;

public enum CdcEventType {
    INSERT("pubinsert"),
    UPDATE("pubupdate"),
    DELETE("pubdelete"),
    UNSUPPORTED("unsupported");
    private final String sqlName;

    CdcEventType(String sqlName) {
        this.sqlName = sqlName;
    }

    public String getSqlName() {
        return sqlName;
    }
}
