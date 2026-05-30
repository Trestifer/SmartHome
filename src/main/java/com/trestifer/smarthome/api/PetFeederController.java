package com.trestifer.smarthome.api;

import com.trestifer.smarthome.petfeeder.PetFeederService;
import com.trestifer.smarthome.petfeeder.dto.CommandStatusRequest;
import com.trestifer.smarthome.petfeeder.dto.DeviceStatusRequest;
import com.trestifer.smarthome.petfeeder.dto.FeedNowRequest;
import com.trestifer.smarthome.petfeeder.dto.ResetWifiRequest;
import com.trestifer.smarthome.petfeeder.dto.ScheduleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class PetFeederController {

	private final PetFeederService service;

	public PetFeederController(PetFeederService service) {
		this.service = service;
	}

	@GetMapping("/{deviceCode}")
	public Object getDevice(@PathVariable String deviceCode) {
		return service.getDevice(deviceCode);
	}

	@PatchMapping("/{deviceCode}/status")
	public Object updateDeviceStatus(
			@PathVariable String deviceCode,
			@Valid @RequestBody DeviceStatusRequest request
	) {
		return service.updateDeviceStatus(deviceCode, request);
	}

	@PostMapping("/{deviceCode}/commands/feed-now")
	@ResponseStatus(HttpStatus.CREATED)
	public Object feedNow(@PathVariable String deviceCode, @Valid @RequestBody FeedNowRequest request) {
		return service.feedNow(deviceCode, request);
	}

	@GetMapping("/{deviceCode}/schedules")
	public Object listSchedules(@PathVariable String deviceCode) {
		return service.listSchedules(deviceCode);
	}

	@PostMapping("/{deviceCode}/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	public Object createSchedule(@PathVariable String deviceCode, @Valid @RequestBody ScheduleRequest request) {
		return service.createSchedule(deviceCode, request);
	}

	@GetMapping("/{deviceCode}/schedules/{scheduleId}")
	public Object getSchedule(@PathVariable String deviceCode, @PathVariable long scheduleId) {
		return service.getSchedule(deviceCode, scheduleId);
	}

	@PatchMapping("/{deviceCode}/schedules/{scheduleId}")
	public Object updateSchedule(
			@PathVariable String deviceCode,
			@PathVariable long scheduleId,
			@Valid @RequestBody ScheduleRequest request
	) {
		return service.updateSchedule(deviceCode, scheduleId, request);
	}

	@DeleteMapping("/{deviceCode}/schedules/{scheduleId}")
	public Object deleteSchedule(@PathVariable String deviceCode, @PathVariable long scheduleId) {
		return service.deleteSchedule(deviceCode, scheduleId);
	}

	@GetMapping("/{deviceCode}/feeding-logs")
	public Object listFeedingLogs(
			@PathVariable String deviceCode,
			@RequestParam(defaultValue = "20") int limit
	) {
		return service.listFeedingLogs(deviceCode, limit);
	}

	@GetMapping("/{deviceCode}/device-logs")
	public Object listDeviceLogs(
			@PathVariable String deviceCode,
			@RequestParam(defaultValue = "50") int limit
	) {
		return service.listDeviceLogs(deviceCode, limit);
	}

	@PostMapping("/{deviceCode}/commands/reset-wifi")
	@ResponseStatus(HttpStatus.CREATED)
	public Object resetWifi(@PathVariable String deviceCode, @Valid @RequestBody ResetWifiRequest request) {
		return service.resetWifi(deviceCode, request);
	}

	@GetMapping("/{deviceCode}/commands")
	public Object listCommands(@PathVariable String deviceCode, @RequestParam(required = false) String status) {
		return service.listCommands(deviceCode, status);
	}

	@PatchMapping("/{deviceCode}/commands/{commandId}")
	public Object updateCommandStatus(
			@PathVariable String deviceCode,
			@PathVariable long commandId,
			@Valid @RequestBody CommandStatusRequest request
	) {
		return service.updateCommandStatus(deviceCode, commandId, request);
	}
}
