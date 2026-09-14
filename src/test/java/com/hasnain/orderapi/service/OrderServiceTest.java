package com.hasnain.orderapi.service;

import com.hasnain.orderapi.dto.CreateOrderRequest;
import com.hasnain.orderapi.dto.OrderResponse;
import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.entity.Product;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.exception.InsufficientStockException;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.messaging.OrderCreatedEvent;
import com.hasnain.orderapi.messaging.RabbitMQConfig;
import com.hasnain.orderapi.repository.OrderRepository;
import com.hasnain.orderapi.repository.ProductRepository;
import com.hasnain.orderapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    private User existingUser() {
        User user = new User();
        user.setId(3L);
        user.setEmail("john@example.com");
        return user;
    }

    private Product productWithStock(int stockQuantity) {
        Product product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("89.99"));
        product.setStockQuantity(stockQuantity);
        return product;
    }

    @Test
    void createOrder_decrementsStockAndCalculatesTotal_andPublishesEvent_whenValid() {
        User user = existingUser();
        Product product = productWithStock(10);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        CreateOrderRequest request = new CreateOrderRequest(Map.of(1L, 2));

        OrderResponse response = orderService.createOrder("john@example.com", request);

        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(response.total()).isEqualByComparingTo("179.98");
        assertThat(response.status()).isEqualTo(OrderStatus.PROCESSING);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(100L);
    }

    @Test
    void createOrder_throwsInsufficientStockException_andNeverSavesOrderOrPublishesEvent_whenStockTooLow() {
        User user = existingUser();
        Product product = productWithStock(1);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        CreateOrderRequest request = new CreateOrderRequest(Map.of(1L, 5));

        assertThatThrownBy(() -> orderService.createOrder("john@example.com", request))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void createOrder_throwsResourceNotFoundException_andNeverTouchesProducts_whenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(Map.of(1L, 1));

        assertThatThrownBy(() -> orderService.createOrder("ghost@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void createOrder_throwsResourceNotFoundException_whenProductDoesNotExist() {
        User user = existingUser();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(Map.of(99L, 1));

        assertThatThrownBy(() -> orderService.createOrder("john@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_returnsMappedResponse_whenOrderExists() {
        User user = existingUser();

        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setTotal(new BigDecimal("89.99"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(3L);
        assertThat(response.userEmail()).isEqualTo("john@example.com");
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getOrderById_throwsResourceNotFoundException_whenOrderDoesNotExist() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllOrders_mapsPageCorrectly_includingPaginationMetadata() {
        User user = existingUser();

        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setTotal(new BigDecimal("89.99"));

        PageImpl<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedResponse<OrderResponse> response = orderService.getAllOrders(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(10L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();
    }
}
