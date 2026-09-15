package com.hasnain.orderapi.messaging;

import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.entity.Role;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.repository.OrderRepository;
import com.hasnain.orderapi.repository.UserRepository;
import com.hasnain.orderapi.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class OrderMessagingIT extends AbstractPostgresContainerTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private Order existingOrder() {
        User user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("hashed-value");
        user.setRole(Role.USER);
        User savedUser = userRepository.save(user);

        Order order = new Order();
        order.setUser(savedUser);
        order.setTotal(new BigDecimal("89.99"));
        return orderRepository.save(order);
    }

    @Test
    void orderCreatedEvent_isConsumedThroughRealBroker_andMovesOrderPastProcessing() {
        Order order = existingOrder();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                new OrderCreatedEvent(order.getId())
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isIn(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED);
        });
    }

    @Test
    void orderCreatedEvent_forNonExistentOrder_exhaustsRetriesAndLandsOnDeadLetterQueue() {
        Long nonExistentOrderId = 999_999L;

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                new OrderCreatedEvent(nonExistentOrderId)
        );

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            OrderCreatedEvent deadLettered = rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.DLQ_QUEUE,
                    new ParameterizedTypeReference<OrderCreatedEvent>() {}
            );
            assertThat(deadLettered).isNotNull();
            assertThat(deadLettered.orderId()).isEqualTo(nonExistentOrderId);
        });
    }
}
