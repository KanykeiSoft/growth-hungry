package com.example.growth_hungry;

import com.example.growth_hungry.config.AiProps;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GrowthHungryApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrowthHungryApplication.class, args);

	}
}
