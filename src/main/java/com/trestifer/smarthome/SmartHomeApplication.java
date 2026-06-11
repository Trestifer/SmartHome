package com.trestifer.smarthome;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SmartHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartHomeApplication.class, args);
	}

	@Bean
	public CommandLineRunner initDatabase(JdbcTemplate jdbc) {
		return args -> {
			try {
				int updated = jdbc.update("UPDATE devices SET device_name = 'feederv0' WHERE device_code = 'PF001'");
				System.out.println("[DB INIT] Updated " + updated + " device name to feederv0");
			} catch (Exception e) {
				System.err.println("[DB INIT] Failed to update device name: " + e.getMessage());
			}
		};
	}
}
