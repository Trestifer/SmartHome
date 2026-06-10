package com.trestifer.smarthome.petfeeder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "smarthome.pet-feeder.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledFeedingRunner {

	private final PetFeederService service;

	public ScheduledFeedingRunner(PetFeederService service) {
		this.service = service;
	}

	@Scheduled(fixedRateString = "${smarthome.pet-feeder.scheduler.fixed-rate-ms:60000}")
	public void checkSchedules() {
		service.enqueueDueScheduledFeeds();
	}
}
