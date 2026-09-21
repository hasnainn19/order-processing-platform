package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.security.JwtService;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * mocks required to bootstrap any @WebMvcTest context here, JwtAuthenticationFilter is a scanned
 * filter bean needing JwtService, and @EnableCaching needs some CacheManager to exist
 */
abstract class ControllerTestSupport {

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CacheManager cacheManager;
}
