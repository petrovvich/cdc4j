package org.savedoc.cdc4j.common;

import java.util.Optional;

public interface CdcClient {

    void start();

    Optional<CdcEvent> read();
}
