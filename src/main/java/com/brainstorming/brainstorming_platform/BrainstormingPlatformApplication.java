package com.brainstorming.brainstorming_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing // Auditing켜는 기능
@SpringBootApplication
public class BrainstormingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrainstormingPlatformApplication.class, args);
	}

}
