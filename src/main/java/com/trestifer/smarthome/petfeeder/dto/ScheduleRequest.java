package com.trestifer.smarthome.petfeeder.dto;

public record ScheduleRequest(
		String feed_time,
		String portion_size,
		Boolean is_active
) {
}
