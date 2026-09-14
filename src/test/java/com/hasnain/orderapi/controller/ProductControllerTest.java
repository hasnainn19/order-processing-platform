package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.dto.ProductResponse;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse aKeyboard() {
        return new ProductResponse(1L, "Mechanical Keyboard", new BigDecimal("89.99"), 10);
    }

    @Test
    void getAllProducts_usesDefaultPaging_whenNoQueryParamsProvided() throws Exception {
        PagedResponse<ProductResponse> page = new PagedResponse<>(List.of(aKeyboard()), 0, 20, 1, 1, true);
        when(productService.getAllProducts(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getAllProducts_passesProvidedPageAndSizeQueryParams() throws Exception {
        PagedResponse<ProductResponse> page = new PagedResponse<>(List.of(), 2, 5, 0, 0, true);
        when(productService.getAllProducts(2, 5)).thenReturn(page);

        mockMvc.perform(get("/api/products").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getProductById_returnsProduct_whenFound() throws Exception {
        when(productService.getProductById(1L)).thenReturn(aKeyboard());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    void getProductById_returns404_whenProductDoesNotExist() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void createProduct_returns200WithCreatedProduct_whenRequestIsValid() throws Exception {
        when(productService.createProduct(any())).thenReturn(
                new ProductResponse(5L, "Gaming Mouse", new BigDecimal("39.99"), 25));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gaming Mouse\",\"price\":39.99,\"stockQuantity\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Gaming Mouse"));
    }

    @Test
    void createProduct_returns400WithFieldErrors_whenNamePriceAndStockAreInvalid() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-5,\"stockQuantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").value("Stock quantity cannot be negative"));
    }
}
