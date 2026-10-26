package com.fina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinaApplication.class, args);
	}

}
