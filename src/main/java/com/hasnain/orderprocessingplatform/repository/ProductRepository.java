package com.hasnain.orderprocessingplatform.repository;

import com.hasnain.orderprocessingplatform.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}