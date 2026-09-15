package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.dto.CreateOrderRequest;
import com.hasnain.orderapi.dto.OrderResponse;
import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse anOrder() {
        return new OrderResponse(10L, 3L, "john@example.com", OrderStatus.PROCESSING,
                new BigDecimal("89.99"), LocalDateTime.now(), List.of());
    }

    private Authentication authenticatedAsJohn() {
        return new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
    }

    @Test
    void getOrderById_returnsOrder_whenFound() throws Exception {
        when(orderService.getOrderById(10L)).thenReturn(anOrder());

        mockMvc.perform(get("/api/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userEmail").value("john@example.com"));
    }

    @Test
    void getOrderById_returns404_whenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById(404L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 404"));

        mockMvc.perform(get("/api/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 404"));
    }

    @Test
    void getAllOrders_usesDefaultPaging_whenNoQueryParamsProvided() throws Exception {
        PagedResponse<OrderResponse> page = new PagedResponse<>(List.of(anOrder()), 0, 20, 1, 1, true);
        when(orderService.getAllOrders(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getAllOrders_passesProvidedPageAndSizeQueryParams() throws Exception {
        PagedResponse<OrderResponse> page = new PagedResponse<>(List.of(), 1, 10, 0, 0, true);
        when(orderService.getAllOrders(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/orders").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void createOrder_passesAuthenticatedUsersEmailAndRequestBody_toService() throws Exception {
        when(orderService.createOrder(eq("john@example.com"), any())).thenReturn(anOrder());

        mockMvc.perform(post("/api/orders")
                        .principal(authenticatedAsJohn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":{\"1\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        verify(orderService).createOrder(eq("john@example.com"), argThat((CreateOrderRequest request) ->
                request.items().equals(Map.of(1L, 2))));
    }

    @Test
    void createOrder_returns400WithFieldError_whenItemsMapIsEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .principal(authenticatedAsJohn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }
}
