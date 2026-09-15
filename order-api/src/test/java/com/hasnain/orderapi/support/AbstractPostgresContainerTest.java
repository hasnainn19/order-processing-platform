package com.hasnain.orderapi.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Postgres container + CacheManager mock every real-database integration test needs to boot
 * @EnableCaching on the app class requires some CacheManager even when a test has nothing to do with caching.
 */
public abstract class AbstractPostgresContainerTest {

    @Container
    @ServiceConnection
    protected static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @MockitoBean
    protected CacheManager cacheManager;
}
