package com.hasnain.orderapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @Mock
    private PrintWriter writer;

    @InjectMocks
    private JwtAuthenticationEntryPoint entryPoint;

    @Test
    void commence_writesA401JsonBody_withStatusAndContentTypeSet() throws Exception {
        when(response.getWriter()).thenReturn(writer);

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"error\":\"Unauthorized\"");
        assertThat(body).contains("\"message\":\"Authentication is required to access this resource\"");
    }
}
