package com.hasnain.paymentworker.messaging;

import com.hasnain.paymentworker.payment.PaymentGateway;
import com.hasnain.paymentworker.payment.PaymentResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private final PaymentGateway paymentGateway;
    private final RabbitTemplate rabbitTemplate;

    public OrderCreatedEventListener(PaymentGateway paymentGateway, RabbitTemplate rabbitTemplate) {
        this.paymentGateway = paymentGateway;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        PaymentResult result = paymentGateway.charge(event.orderId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_PROCESSED_EXCHANGE,
                RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                new PaymentProcessedEvent(event.orderId(), result.succeeded(), result.failureReason())
        );
    }
}
