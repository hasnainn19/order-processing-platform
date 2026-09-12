package com.hasnain.orderprocessingplatform.controller;

import com.hasnain.orderprocessingplatform.service.ProductService;

import jakarta.validation.Valid;

import com.hasnain.orderprocessingplatform.dto.ProductResponse;
import com.hasnain.orderprocessingplatform.dto.CreateProductRequest;
import com.hasnain.orderprocessingplatform.dto.PagedResponse;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController 
@RequestMapping("/api/products")
public class ProductController {
    
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PagedResponse<ProductResponse> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return productService.getAllProducts(page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public ProductResponse createProduct(@RequestBody @Valid CreateProductRequest request) {
        return productService.createProduct(request);
    } 
}
