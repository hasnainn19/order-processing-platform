package com.hasnain.orderapi.seed;

import tools.jackson.databind.ObjectMapper;
import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.OrderItem;
import com.hasnain.orderapi.entity.OrderStatus;
import com.hasnain.orderapi.entity.Product;
import com.hasnain.orderapi.entity.Role;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.repository.OrderRepository;
import com.hasnain.orderapi.repository.ProductRepository;
import com.hasnain.orderapi.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
/**
 * kept out of the test suite this way, the actual test profile activation
 * happens via surefire/failsafe systemPropertyVariables in pom.xml, not a properties
 * file, see pom.xml for why
 */
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password123";
    private static final int CONFIRMED_ORDERS_TO_SEED = 30;
    private static final int PAYMENT_FAILED_ORDERS_TO_SEED = 5;

    private static final List<String> DEMO_USER_NAMES = List.of(
        "john.doe", "jane.doe", "michael.smith", "emily.johnson", "chris.lee",
        "sarah.brown", "david.wilson", "laura.davis", "james.miller", "anna.garcia"
    );

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(UserRepository userRepository, ProductRepository productRepository,
            OrderRepository orderRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws IOException {
        // bail out if this already ran, otherwise restarting the app would duplicate
        // everything and blow up on the unique email constraint
        if (productRepository.count() > 0) {
            return;
        }

        seedAdmin();
        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        seedOrders(users, products);
    }

    private void seedAdmin() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private List<User> seedUsers() {
        return DEMO_USER_NAMES.stream()
            .map(name -> {
                User user = new User();
                user.setEmail(name + "@example.com");
                user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                user.setRole(Role.USER);
                return userRepository.save(user);
            })
            .toList();
    }

    private List<Product> seedProducts() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/products.json");
        Product[] products = objectMapper.readValue(resource.getInputStream(), Product[].class);
        return productRepository.saveAll(List.of(products));
    }

    private void seedOrders(List<User> users, List<Product> products) {
        // fixed seed so the demo data is reproducible across restarts, not a leftover debug value
        Random random = new Random(42);

        for (int i = 0; i < CONFIRMED_ORDERS_TO_SEED; i++) {
            orderRepository.save(buildOrder(users, products, random, OrderStatus.CONFIRMED));
        }
        for (int i = 0; i < PAYMENT_FAILED_ORDERS_TO_SEED; i++) {
            orderRepository.save(buildOrder(users, products, random, OrderStatus.PAYMENT_FAILED));
        }
    }

    /**
     * orders are built directly with a final status instead of going through the real event
     * flow, since the random simulated gateway can't be made to hit an exact 30/5 split
     */
    private Order buildOrder(List<User> users, List<Product> products, Random random, OrderStatus status) {
        User user = users.get(random.nextInt(users.size()));
        Product product = products.get(random.nextInt(products.size()));
        int quantity = random.nextInt(3) + 1;

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPrice(product.getPrice());

        Order order = new Order();
        order.setUser(user);
        order.setStatus(status);
        order.setTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.addItem(item);

        return order;
    }
}
