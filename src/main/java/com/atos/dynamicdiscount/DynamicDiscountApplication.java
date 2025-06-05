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
/*	@Bean
	public CommandLineRunner runDiscountManager(ExecutionManager discountManager) {
		return args -> {
			String billCycle = "90";
			try {
				discountManager.processDiscounts(billCycle);
			} catch (Exception e) {
				System.err.println("Error during discount processing: " + e.getMessage());
				e.printStackTrace();
			}
		};
	}
	*/

}
