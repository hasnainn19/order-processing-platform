package com.hasnain.orderapi.repository;

import com.hasnain.orderapi.entity.Product;
import com.hasnain.orderapi.exception.InsufficientStockException;
import com.hasnain.orderapi.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
/**
 * keeps the real postgres container instead of @DataJpaTest's default embedded database swap,
 * locking semantics like the pessimistic lock below don't reliably behave the same on h2
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductRepositoryContainerIT.StockDecrementer.class)
class ProductRepositoryContainerIT extends AbstractPostgresContainerTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockDecrementer stockDecrementer;

    @Test
    void save_thenFindById_returnsProductWithMatchingFields() {
        Product product = new Product();
        product.setName("Mechanical Keyboard");
        product.setPrice(new BigDecimal("89.99"));
        product.setStockQuantity(10);

        Product saved = productRepository.save(product);

        Optional<Product> found = productRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mechanical Keyboard");
        assertThat(found.get().getPrice()).isEqualByComparingTo("89.99");
        assertThat(found.get().getStockQuantity()).isEqualTo(10);
    }

    @Test
    /**
     * opted out of the transaction @DataJpaTest normally wraps every test in, the two threads
     * below need genuinely separate transactions racing each other, a single wrapping transaction
     * would defeat the pessimistic lock this test is trying to prove
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByIdForUpdate_underConcurrentAccess_allowsOnlyOneDecrementToSucceed() throws Exception {
        Product product = new Product();
        product.setName("Limited Stock Item");
        product.setPrice(new BigDecimal("19.99"));
        product.setStockQuantity(1);
        Long productId = productRepository.save(product).getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<String> attemptPurchase = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                stockDecrementer.decrementStock(productId, 1);
                return "SUCCESS";
            }
            catch (InsufficientStockException e) {
                return "FAILED";
            }
        };

        Future<String> result1 = executor.submit(attemptPurchase);
        Future<String> result2 = executor.submit(attemptPurchase);

        readyLatch.await();
        startLatch.countDown();

        List<String> outcomes = List.of(
                result1.get(5, TimeUnit.SECONDS),
                result2.get(5, TimeUnit.SECONDS)
        );
        executor.shutdown();

        assertThat(outcomes).containsExactlyInAnyOrder("SUCCESS", "FAILED");

        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0);
    }

    /**
     * a separate component so decrementStock runs through a real spring proxy, each thread above
     * needs its own genuine transaction, which a plain method call from the test itself wouldn't get
     */
    @Component
    static class StockDecrementer {

        private final ProductRepository productRepository;

        StockDecrementer(ProductRepository productRepository) {
            this.productRepository = productRepository;
        }

        @Transactional
        void decrementStock(Long productId, int quantity) {
            Product product = productRepository.findByIdForUpdate(productId).orElseThrow();

            if (product.getStockQuantity() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + productId);
            }

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }
    }
}
