package com.trestifer.smarthome.api;

import jakarta.validation.constraints.NotBlank;

public record Device(
		long id,
		@NotBlank String name,
		@NotBlank String room,
		boolean online
) {
}
