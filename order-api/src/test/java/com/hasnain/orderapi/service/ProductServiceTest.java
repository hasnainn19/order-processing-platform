package com.hasnain.orderapi.service;

import com.hasnain.orderapi.dto.CreateProductRequest;
import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.dto.ProductResponse;
import com.hasnain.orderapi.entity.Product;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductById_returnsMappedResponse_whenProductExists() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Mechanical Keyboard");
        product.setPrice(new BigDecimal("89.99"));
        product.setStockQuantity(10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mechanical Keyboard");
        assertThat(response.price()).isEqualByComparingTo("89.99");
        assertThat(response.stockQuantity()).isEqualTo(10);
    }

    @Test
    void getProductById_throwsResourceNotFoundException_whenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createProduct_savesEntityBuiltFromRequest_andReturnsMappedResponse() {
        CreateProductRequest request = new CreateProductRequest("Gaming Mouse", new BigDecimal("39.99"), 25);

        Product savedProduct = new Product();
        savedProduct.setId(5L);
        savedProduct.setName(request.name());
        savedProduct.setPrice(request.price());
        savedProduct.setStockQuantity(request.stockQuantity());

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product productPassedToRepository = productCaptor.getValue();

        assertThat(productPassedToRepository.getName()).isEqualTo("Gaming Mouse");
        assertThat(productPassedToRepository.getPrice()).isEqualByComparingTo("39.99");
        assertThat(productPassedToRepository.getStockQuantity()).isEqualTo(25);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Gaming Mouse");
    }

    @Test
    void getAllProducts_mapsPageCorrectly_includingPaginationMetadata() {
        Product product = new Product();
        product.setId(1L);
        product.setName("USB-C Hub");
        product.setPrice(new BigDecimal("24.99"));
        product.setStockQuantity(14);

        PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedResponse<ProductResponse> response = productService.getAllProducts(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("USB-C Hub");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();
    }
}
