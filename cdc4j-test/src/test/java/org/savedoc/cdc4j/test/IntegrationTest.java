package org.savedoc.cdc4j.test;

import org.savedoc.cdc4j.common.ValidationUtils;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class IntegrationTest {

    protected PostgreSQLContainer<?> buildPostgres(String dockerImage, String sqlOnStartupPath) {
        final var postgres = new PostgreSQLContainer<>(dockerImage);
        if (ValidationUtils.isNotEmpty(sqlOnStartupPath)) {
            postgres.withInitScript(sqlOnStartupPath);
        }
        postgres.start();
        return postgres;
    }
}
