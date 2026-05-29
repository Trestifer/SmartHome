package com.trestifer.smarthome.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final AtomicLong ids = new AtomicLong(3);
	private final Map<Long, Device> devices = new LinkedHashMap<>(Map.of(
			1L, new Device(1, "Living Room Light", "Living Room", true),
			2L, new Device(2, "Bedroom Thermostat", "Bedroom", true),
			3L, new Device(3, "Garage Door", "Garage", false)
	));

	@GetMapping
	public Collection<Device> listDevices() {
		return devices.values();
	}

	@GetMapping("/{id}")
	public Device getDevice(@PathVariable long id) {
		Device device = devices.get(id);
		if (device == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found");
		}
		return device;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Device createDevice(@Valid @RequestBody Device request) {
		long id = ids.incrementAndGet();
		Device device = new Device(id, request.name(), request.room(), request.online());
		devices.put(id, device);
		return device;
	}
}
