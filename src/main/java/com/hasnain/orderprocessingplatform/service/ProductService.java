package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.entity.Product;
import com.hasnain.orderprocessingplatform.repository.ProductRepository;
import com.hasnain.orderprocessingplatform.dto.ProductResponse;
import com.hasnain.orderprocessingplatform.dto.CreateProductRequest;

import org.springframework.stereotype.Service;

import java.util.List;

@Service 
public class ProductService {

    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        return toResponse(product);
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStockQuantity()
        );
    }
}
