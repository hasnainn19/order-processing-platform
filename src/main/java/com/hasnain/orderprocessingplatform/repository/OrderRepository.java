package com.hasnain.orderprocessingplatform.repository;

import com.hasnain.orderprocessingplatform.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}
