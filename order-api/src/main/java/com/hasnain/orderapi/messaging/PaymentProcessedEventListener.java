package com.hasnain.orderapi.messaging;

import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessedEventListener {

    private final OrderRepository orderRepository;

    public PaymentProcessedEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_PROCESSED_QUEUE)
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + event.orderId()));

        if (event.paymentSucceeded()) {
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
