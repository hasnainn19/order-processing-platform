package com.hasnain.orderapi.messaging;

import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessedEventListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentProcessedEventListener listener;

    private Order existingOrder() {
        Order order = new Order();
        order.setId(10L);
        return order;
    }

    @Test
    void handlePaymentProcessed_withSuccessfulPayment_movesOrderThroughPaidToConfirmed() {
        Order order = existingOrder();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        listener.handlePaymentProcessed(new PaymentProcessedEvent(10L, true, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    void handlePaymentProcessed_withFailedPayment_movesOrderToPaymentFailed() {
        Order order = existingOrder();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        listener.handlePaymentProcessed(new PaymentProcessedEvent(10L, false, "card_declined"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void handlePaymentProcessed_throwsResourceNotFoundException_andNeverSaves_whenOrderDoesNotExist() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.handlePaymentProcessed(new PaymentProcessedEvent(404L, true, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }
}
