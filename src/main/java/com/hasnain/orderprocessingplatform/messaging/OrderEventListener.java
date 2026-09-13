package com.hasnain.orderprocessingplatform.messaging;

import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.entity.OrderStatus;
import com.hasnain.orderprocessingplatform.exception.ResourceNotFoundException;
import com.hasnain.orderprocessingplatform.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class OrderEventListener {
    
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    public OrderEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + event.orderId()));

        boolean paymentSucceeded = random.nextInt(100) >= 15;

        if (paymentSucceeded) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }
    }
}
