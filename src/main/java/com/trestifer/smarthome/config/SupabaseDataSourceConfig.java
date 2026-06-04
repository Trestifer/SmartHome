package com.trestifer.smarthome.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty("spring.datasource.url")
public class SupabaseDataSourceConfig {

	@Bean
	DataSource dataSource(
			@Value("${spring.datasource.url}") String url,
			@Value("${spring.datasource.username}") String username,
			@Value("${spring.datasource.password}") String password,
			@Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName,
			@Value("${spring.datasource.hikari.initialization-fail-timeout:-1}") long initializationFailTimeout
	) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		config.setUsername(username);
		config.setPassword(password);
		config.setDriverClassName(driverClassName);
		config.setInitializationFailTimeout(initializationFailTimeout);
		return new HikariDataSource(config);
	}
}
