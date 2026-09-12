package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.entity.Product;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.entity.OrderItem;
import com.hasnain.orderprocessingplatform.repository.OrderRepository;
import com.hasnain.orderprocessingplatform.repository.ProductRepository;
import com.hasnain.orderprocessingplatform.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional 
    public Order createOrder(Long userId, Map<Long, Integer> productIdToQuantity) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Order order = new Order();
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productIdToQuantity.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for product: " + productId);
            }

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(product.getPrice());
            order.addItem(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);
        return orderRepository.save(order);  
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
}
