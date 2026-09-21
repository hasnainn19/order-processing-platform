package com.hasnain.orderapi.repository;

import com.hasnain.orderapi.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * the underscore here is spring data traversing order.user.email
     */ 
    Page<Order> findByUser_Email(String email, Pageable pageable);
}
    