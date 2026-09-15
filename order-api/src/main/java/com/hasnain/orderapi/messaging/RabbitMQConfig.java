package com.hasnain.orderapi.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_CREATED_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String PAYMENT_PROCESSED_EXCHANGE = "payment-processed.exchange";
    public static final String PAYMENT_PROCESSED_QUEUE = "payment-processed.queue";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";

    public static final String PAYMENT_PROCESSED_DLX = "payment-processed.dlx";
    public static final String PAYMENT_PROCESSED_DLQ = "payment-processed.dlq";
    public static final String PAYMENT_PROCESSED_DLQ_ROUTING_KEY = "payment.processed.failed";

    @Bean
    DirectExchange orderCreatedExchange() {
        return new DirectExchange(ORDER_CREATED_EXCHANGE);
    }

    @Bean
    Queue paymentProcessedQueue() {
        return new Queue(PAYMENT_PROCESSED_QUEUE, true);
    }

    @Bean
    Queue paymentProcessedDeadLetterQueue() {
        return new Queue(PAYMENT_PROCESSED_DLQ, true);
    }

    @Bean
    DirectExchange paymentProcessedExchange() {
        return new DirectExchange(PAYMENT_PROCESSED_EXCHANGE);
    }

    @Bean
    DirectExchange paymentProcessedDeadLetterExchange() {
        return new DirectExchange(PAYMENT_PROCESSED_DLX);
    }

    @Bean
    Binding paymentProcessedBinding(Queue paymentProcessedQueue, DirectExchange paymentProcessedExchange) {
        return BindingBuilder.bind(paymentProcessedQueue).to(paymentProcessedExchange).with(PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    Binding paymentProcessedDeadLetterBinding(Queue paymentProcessedDeadLetterQueue, DirectExchange paymentProcessedDeadLetterExchange) {
        return BindingBuilder.bind(paymentProcessedDeadLetterQueue).to(paymentProcessedDeadLetterExchange).with(PAYMENT_PROCESSED_DLQ_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, PAYMENT_PROCESSED_DLX, PAYMENT_PROCESSED_DLQ_ROUTING_KEY);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter,
        MessageRecoverer messageRecoverer
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        Advice retryInterceptor = RetryInterceptorBuilder.stateless()
            .maxRetries(3)
            .backOffOptions(1000, 2.0, 10000)
            .recoverer(messageRecoverer)
            .build();

        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
