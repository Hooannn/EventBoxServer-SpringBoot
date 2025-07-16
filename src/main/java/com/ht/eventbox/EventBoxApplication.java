package com.ht.eventbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventBoxApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventBoxApplication.class, args);
	}

}