package com.hasnain.paymentworker.messaging;

import com.hasnain.paymentworker.payment.PaymentGateway;
import com.hasnain.paymentworker.payment.PaymentProviderException;
import com.hasnain.paymentworker.payment.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Import(OrderCreatedMessagingIT.VerificationQueueConfig.class)
class OrderCreatedMessagingIT {

    // Declared by order-api, which owns this queue as its consumer; payment-worker only publishes to the exchange.
    private static final String PAYMENT_PROCESSED_QUEUE = "payment-processed.queue";

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private void publishOrderCreated(Long orderId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_CREATED_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                new OrderCreatedEvent(orderId)
        );
    }

    private PaymentProcessedEvent awaitPublishedPaymentProcessedEvent() {
        return await().atMost(Duration.ofSeconds(5)).until(
                () -> rabbitTemplate.receiveAndConvert(PAYMENT_PROCESSED_QUEUE, new ParameterizedTypeReference<PaymentProcessedEvent>() {}),
                Objects::nonNull
        );
    }

    @Test
    void orderCreatedEvent_withSuccessfulPayment_publishesSuccessfulPaymentProcessedEvent() {
        when(paymentGateway.charge(anyLong())).thenReturn(PaymentResult.success());

        publishOrderCreated(1L);
        PaymentProcessedEvent published = awaitPublishedPaymentProcessedEvent();

        assertThat(published.orderId()).isEqualTo(1L);
        assertThat(published.paymentSucceeded()).isTrue();
    }

    @Test
    void orderCreatedEvent_withDeclinedPayment_publishesDeclinedPaymentProcessedEvent() {
        when(paymentGateway.charge(anyLong())).thenReturn(PaymentResult.declined("card_declined"));

        publishOrderCreated(2L);
        PaymentProcessedEvent published = awaitPublishedPaymentProcessedEvent();

        assertThat(published.paymentSucceeded()).isFalse();
        assertThat(published.failureReason()).isEqualTo("card_declined");
    }

    @Test
    void orderCreatedEvent_whenProviderUnreachable_exhaustsRetriesAndLandsOnDeadLetterQueue() {
        when(paymentGateway.charge(anyLong())).thenThrow(new PaymentProviderException("unreachable"));

        publishOrderCreated(3L);

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            OrderCreatedEvent deadLettered = rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.ORDER_CREATED_DLQ,
                    new ParameterizedTypeReference<OrderCreatedEvent>() {}
            );
            assertThat(deadLettered).isNotNull();
            assertThat(deadLettered.orderId()).isEqualTo(3L);
        });
    }

    // Stands in for order-api, which normally declares and owns this queue as the flow's consumer.
    @TestConfiguration
    static class VerificationQueueConfig {

        @Bean
        Queue paymentProcessedVerificationQueue() {
            return new Queue(PAYMENT_PROCESSED_QUEUE, true);
        }

        @Bean
        Binding paymentProcessedVerificationBinding(Queue paymentProcessedVerificationQueue, DirectExchange paymentProcessedExchange) {
            return BindingBuilder.bind(paymentProcessedVerificationQueue)
                    .to(paymentProcessedExchange)
                    .with(RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY);
        }
    }
}
