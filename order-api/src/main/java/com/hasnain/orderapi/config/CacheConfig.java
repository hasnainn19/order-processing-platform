package com.hasnain.orderapi.config;

import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.dto.ProductResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class CacheConfig {

    /**
     * PagedResponse is generic, so jackson needs the fully parameterized type below
     * to deserialize it back properly instead of collapsing everything into raw maps
     */
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        JavaType productsPageType = objectMapper.getTypeFactory()
                .constructParametricType(PagedResponse.class, ProductResponse.class);

        JacksonJsonRedisSerializer<PagedResponse<ProductResponse>> productsSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, productsPageType);

        RedisCacheConfiguration productsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productsSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("products", productsConfig)
                .build();
    }
}
