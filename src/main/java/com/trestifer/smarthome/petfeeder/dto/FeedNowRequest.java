package com.trestifer.smarthome.petfeeder.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedNowRequest(
		@NotBlank(message = "portion_size là bắt buộc.") String portion_size
) {
}
