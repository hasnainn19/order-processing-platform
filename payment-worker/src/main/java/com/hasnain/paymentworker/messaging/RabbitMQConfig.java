package com.hasnain.paymentworker.messaging;

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
    public static final String ORDER_CREATED_QUEUE = "order.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String ORDER_CREATED_DLX = "order.dlx";
    public static final String ORDER_CREATED_DLQ = "order.dlq";
    public static final String ORDER_CREATED_DLQ_ROUTING_KEY = "order.failed";

    public static final String PAYMENT_PROCESSED_EXCHANGE = "payment-processed.exchange";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";

    @Bean
    Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    Queue orderCreatedDeadLetterQueue() {
        return new Queue(ORDER_CREATED_DLQ, true);
    }

    @Bean
    DirectExchange orderCreatedExchange() {
        return new DirectExchange(ORDER_CREATED_EXCHANGE);
    }

    @Bean
    DirectExchange orderCreatedDeadLetterExchange() {
        return new DirectExchange(ORDER_CREATED_DLX);
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange orderCreatedExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderCreatedExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding orderCreatedDeadLetterBinding(Queue orderCreatedDeadLetterQueue, DirectExchange orderCreatedDeadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue).to(orderCreatedDeadLetterExchange).with(ORDER_CREATED_DLQ_ROUTING_KEY);
    }

    @Bean
    DirectExchange paymentProcessedExchange() {
        return new DirectExchange(PAYMENT_PROCESSED_EXCHANGE);
    }

    @Bean
    MessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, ORDER_CREATED_DLX, ORDER_CREATED_DLQ_ROUTING_KEY);
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
