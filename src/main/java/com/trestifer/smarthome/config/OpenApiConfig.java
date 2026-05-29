package com.trestifer.smarthome.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI smartHomeOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Smart Home API")
						.version("v1")
						.description("REST API for managing smart home devices."));
	}
}
