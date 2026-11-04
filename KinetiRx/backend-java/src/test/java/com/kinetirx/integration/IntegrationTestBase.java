package com.kinetirx.integration;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base for tests that need a real PostgreSQL database.
 *
 * Testcontainers needs Docker, which this environment does not have, so these tests run against
 * a database named by KINETIRX_TEST_DATABASE_URL (a disposable copy, never production). Without
 * that variable they are skipped, not failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "KINETIRX_TEST_DATABASE_URL", matches = ".+")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;
}
