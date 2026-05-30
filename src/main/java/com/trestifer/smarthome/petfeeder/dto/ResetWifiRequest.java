package com.trestifer.smarthome.petfeeder.dto;

import jakarta.validation.constraints.NotNull;

public record ResetWifiRequest(
		@NotNull(message = "confirm là bắt buộc.") Boolean confirm
) {
}
