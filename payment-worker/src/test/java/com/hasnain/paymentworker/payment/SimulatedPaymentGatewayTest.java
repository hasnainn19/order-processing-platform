package com.hasnain.paymentworker.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulatedPaymentGatewayTest {

    @Mock
    private Random random;

    @Test
    void charge_returnsSuccess_whenRollIsBelow70() {
        when(random.nextInt(100)).thenReturn(0);

        PaymentResult result = new SimulatedPaymentGateway(random).charge(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void charge_returnsDeclined_whenRollIsBetween70And89() {
        when(random.nextInt(100)).thenReturn(70);

        PaymentResult result = new SimulatedPaymentGateway(random).charge(1L);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).isEqualTo("card_declined");
    }

    @Test
    void charge_throwsPaymentProviderException_whenRollIs90OrAbove() {
        when(random.nextInt(100)).thenReturn(90);

        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(random);

        assertThatThrownBy(() -> gateway.charge(1L))
                .isInstanceOf(PaymentProviderException.class);
    }
}
