package com.techgrid.slickbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlickbatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlickbatchApplication.class, args);
	}

}
