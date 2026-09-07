package com.quantplatform.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StratlabApplication {

	public static void main(String[] args) {
		SpringApplication.run(StratlabApplication.class, args);
	}

}
