package com.hasnain.orderapi.repository;

import com.hasnain.orderapi.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}
