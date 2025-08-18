package com.abhishek.realtimeinsighthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealtimeinsighthubApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtimeinsighthubApplication.class, args);
	}

}
