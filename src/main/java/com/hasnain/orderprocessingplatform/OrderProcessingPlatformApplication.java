package com.hasnain.orderprocessingplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching 
public class OrderProcessingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderProcessingPlatformApplication.class, args);
	}

}
