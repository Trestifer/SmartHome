package com.trestifer.smarthome.petfeeder.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceStatusRequest(
		@NotBlank(message = "status là bắt buộc.") String status,
		String message
) {
}
