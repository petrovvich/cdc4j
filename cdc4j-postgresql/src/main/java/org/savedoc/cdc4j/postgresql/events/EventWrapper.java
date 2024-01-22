package org.savedoc.cdc4j.postgresql.events;

import org.postgresql.replication.LogSequenceNumber;
import org.savedoc.cdc4j.common.CdcEvent;

public final class EventWrapper {
    private final LogSequenceNumber lastLSN;
    private final CdcEvent event;

    public EventWrapper(LogSequenceNumber lastLSN, CdcEvent event) {
        this.lastLSN = lastLSN;
        this.event = event;
    }

    public LogSequenceNumber getLastLSN() {
        return lastLSN;
    }

    public CdcEvent getEvent() {
        return event;
    }
}
