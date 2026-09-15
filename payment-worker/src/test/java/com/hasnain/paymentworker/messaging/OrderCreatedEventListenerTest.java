package com.hasnain.paymentworker.messaging;

import com.hasnain.paymentworker.payment.PaymentGateway;
import com.hasnain.paymentworker.payment.PaymentProviderException;
import com.hasnain.paymentworker.payment.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderCreatedEventListener listener;

    private PaymentProcessedEvent triggerAndCapturePublishedEvent(Long orderId) {
        listener.handleOrderCreated(new OrderCreatedEvent(orderId));

        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PAYMENT_PROCESSED_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY),
                eventCaptor.capture()
        );
        return eventCaptor.getValue();
    }

    @Test
    void handleOrderCreated_publishesSuccessfulPaymentProcessedEvent_whenGatewayReturnsSuccess() {
        when(paymentGateway.charge(1L)).thenReturn(PaymentResult.success());

        PaymentProcessedEvent published = triggerAndCapturePublishedEvent(1L);

        assertThat(published.orderId()).isEqualTo(1L);
        assertThat(published.paymentSucceeded()).isTrue();
        assertThat(published.failureReason()).isNull();
    }

    @Test
    void handleOrderCreated_publishesDeclinedPaymentProcessedEvent_whenGatewayReturnsDeclined() {
        when(paymentGateway.charge(1L)).thenReturn(PaymentResult.declined("card_declined"));

        PaymentProcessedEvent published = triggerAndCapturePublishedEvent(1L);

        assertThat(published.paymentSucceeded()).isFalse();
        assertThat(published.failureReason()).isEqualTo("card_declined");
    }

    @Test
    void handleOrderCreated_propagatesException_andNeverPublishes_whenGatewayThrows() {
        when(paymentGateway.charge(1L)).thenThrow(new PaymentProviderException("unreachable"));

        assertThatThrownBy(() -> listener.handleOrderCreated(new OrderCreatedEvent(1L)))
                .isInstanceOf(PaymentProviderException.class);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
