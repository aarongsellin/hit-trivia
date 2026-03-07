package com.hittrivia.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HitTriviaApplication {
	public static void main(String[] args) {
		SpringApplication.run(HitTriviaApplication.class, args);
	}

}
