package com.hasnain.orderprocessingplatform.controller;

import com.hasnain.orderprocessingplatform.service.ProductService;

import jakarta.validation.Valid;

import com.hasnain.orderprocessingplatform.dto.ProductResponse;
import com.hasnain.orderprocessingplatform.dto.CreateProductRequest;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController 
@RequestMapping("/api/products")
public class ProductController {
    
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
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
