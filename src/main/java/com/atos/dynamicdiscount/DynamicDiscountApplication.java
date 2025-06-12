package com.atos.dynamicdiscount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class DynamicDiscountApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicDiscountApplication.class, args);
	}
}
