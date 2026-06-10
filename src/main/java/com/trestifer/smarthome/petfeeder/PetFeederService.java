package com.trestifer.smarthome.petfeeder;

import com.trestifer.smarthome.petfeeder.dto.CommandStatusRequest;
import com.trestifer.smarthome.petfeeder.dto.DeviceStatusRequest;
import com.trestifer.smarthome.petfeeder.dto.FeedNowRequest;
import com.trestifer.smarthome.petfeeder.dto.ResetWifiRequest;
import com.trestifer.smarthome.petfeeder.dto.ScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PetFeederService {

	private static final Logger log = LoggerFactory.getLogger(PetFeederService.class);
	private static final Set<String> DEVICE_STATUSES = Set.of("online", "offline", "feeding", "error");
	private static final Set<String> PORTION_SIZES = Set.of("small", "medium", "large");
	private static final Set<String> COMMAND_STATUSES = Set.of("pending", "sent", "success", "failed");
	private static final Set<String> COMMAND_FILTER_STATUSES = COMMAND_STATUSES;
	private static final DateTimeFormatter FEED_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private final PetFeederRepository repository;
	private final DeviceCommandSender commandSender;
	private final ZoneId scheduleZone;

	public PetFeederService(
			PetFeederRepository repository,
			DeviceCommandSender commandSender,
			@Value("${smarthome.pet-feeder.scheduler.zone:Asia/Ho_Chi_Minh}") String scheduleZone
	) {
		this.repository = repository;
		this.commandSender = commandSender;
		this.scheduleZone = ZoneId.of(scheduleZone);
	}

	public Map<String, Object> getDevice(String deviceCode) {
		return repository.findDevice(deviceCode).orElseThrow(() -> notFound("Không tìm thấy thiết bị."));
	}

	@Transactional
	public Map<String, Object> updateDeviceStatus(String deviceCode, DeviceStatusRequest request) {
		ensureDeviceExists(deviceCode);
		requireOneOf(request.status(), DEVICE_STATUSES, "status không hợp lệ.");
		repository.updateDeviceStatus(deviceCode, request.status());
		repository.createDeviceLog(deviceCode, "connection", defaultMessage(request.message(), "Cập nhật trạng thái thiết bị: " + request.status()));
		return Map.of(
				"message", "Cập nhật trạng thái thiết bị thành công",
				"device_code", deviceCode,
				"status", request.status()
		);
	}

	@Transactional
	public Map<String, Object> feedNow(String deviceCode, FeedNowRequest request) {
		ensureDeviceExists(deviceCode);
		requireOneOf(request.portion_size(), PORTION_SIZES, "portion_size phải là small, medium, hoặc large.");
		long commandId = repository.createCommand(deviceCode, "feed_now", request.portion_size());
		repository.createDeviceLog(deviceCode, "command", "Nhận lệnh cho ăn ngay");
		commandSender.sendCommand(deviceCode, commandId, "feed_now", request.portion_size());
		Map<String, Object> command = repository.findCommand(deviceCode, commandId).orElseThrow(() -> notFound("Không tìm thấy lệnh."));
		return Map.of(
				"message", "Đã tạo lệnh cho ăn ngay",
				"command_id", command.get("command_id"),
				"device_code", command.get("device_code"),
				"command_type", command.get("command_type"),
				"portion_size", command.get("portion_size"),
				"status", command.get("status"),
				"created_at", command.get("created_at")
		);
	}

	@Transactional
	public void enqueueDueScheduledFeeds() {
		LocalTime feedTime = LocalTime.now(scheduleZone).withSecond(0).withNano(0);
		List<Map<String, Object>> dueSchedules = repository.listDueSchedules(feedTime);
		if (dueSchedules.isEmpty()) {
			return;
		}
		log.info("Found {} scheduled feed(s) due at {} in {}", dueSchedules.size(), feedTime.format(FEED_TIME_FORMAT), scheduleZone);
		for (Map<String, Object> schedule : dueSchedules) {
			String deviceCode = schedule.get("device_code").toString();
			String portionSize = schedule.get("portion_size").toString();
			if (repository.hasPendingOrSentFeedCommandInCurrentMinute(deviceCode, portionSize)) {
				continue;
			}
			long commandId = repository.createCommand(deviceCode, "feed_now", portionSize);
			repository.createDeviceLog(deviceCode, "schedule", "Created scheduled feed command from schedule " + schedule.get("schedule_id"));
			commandSender.sendCommand(deviceCode, commandId, "feed_now", portionSize);
		}
	}

	public List<Map<String, Object>> listSchedules(String deviceCode) {
		ensureDeviceExists(deviceCode);
		return formatScheduleRows(repository.listSchedules(deviceCode));
	}

	@Transactional
	public Map<String, Object> createSchedule(String deviceCode, ScheduleRequest request) {
		ensureDeviceExists(deviceCode);
		LocalTime feedTime = requireFeedTime(request.feed_time());
		String portionSize = requirePortionSize(request.portion_size());
		if (repository.hasActiveScheduleWithinFiveMinutes(deviceCode, feedTime, null)) {
			throw badRequest("Thời gian cho ăn quá gần với lịch khác. Vui lòng đặt các lịch cách nhau ít nhất 5 phút.");
		}
		long scheduleId = repository.createSchedule(deviceCode, feedTime, portionSize);
		Map<String, Object> schedule = formatSchedule(repository.findSchedule(deviceCode, scheduleId).orElseThrow(() -> notFound("Không tìm thấy lịch cho ăn.")));
		return Map.of("message", "Tạo lịch cho ăn thành công", "schedule", schedule);
	}

	public Map<String, Object> getSchedule(String deviceCode, long scheduleId) {
		ensureDeviceExists(deviceCode);
		return formatSchedule(repository.findSchedule(deviceCode, scheduleId).orElseThrow(() -> notFound("Không tìm thấy lịch cho ăn.")));
	}

	@Transactional
	public Map<String, Object> updateSchedule(String deviceCode, long scheduleId, ScheduleRequest request) {
		ensureDeviceExists(deviceCode);
		Map<String, Object> current = repository.findSchedule(deviceCode, scheduleId).orElseThrow(() -> notFound("Không tìm thấy lịch cho ăn."));
		LocalTime feedTime = request.feed_time() == null ? toLocalTime(current.get("feed_time")) : requireFeedTime(request.feed_time());
		String portionSize = request.portion_size() == null ? current.get("portion_size").toString() : requirePortionSize(request.portion_size());
		boolean active = request.is_active() == null ? Boolean.TRUE.equals(current.get("is_active")) : request.is_active();
		if (active && repository.hasActiveScheduleWithinFiveMinutes(deviceCode, feedTime, scheduleId)) {
			throw badRequest("Không thể cập nhật lịch vì thời gian mới quá gần với lịch khác.");
		}
		repository.updateSchedule(scheduleId, feedTime, portionSize, active);
		Map<String, Object> schedule = formatSchedule(repository.findSchedule(deviceCode, scheduleId).orElseThrow(() -> notFound("Không tìm thấy lịch cho ăn.")));
		return Map.of("message", "Cập nhật lịch cho ăn thành công", "schedule", schedule);
	}

	@Transactional
	public Map<String, String> deleteSchedule(String deviceCode, long scheduleId) {
		ensureDeviceExists(deviceCode);
		if (repository.findSchedule(deviceCode, scheduleId).isEmpty()) {
			throw notFound("Không tìm thấy lịch cho ăn.");
		}
		repository.deleteSchedule(deviceCode, scheduleId);
		return Map.of("message", "Xóa lịch cho ăn thành công");
	}

	public List<Map<String, Object>> listFeedingLogs(String deviceCode, int limit) {
		ensureDeviceExists(deviceCode);
		return repository.listFeedingLogs(deviceCode, normalizeLimit(limit, 20, 100));
	}

	public List<Map<String, Object>> listDeviceLogs(String deviceCode, int limit) {
		ensureDeviceExists(deviceCode);
		return repository.listDeviceLogs(deviceCode, normalizeLimit(limit, 50, 200));
	}

	@Transactional
	public Map<String, Object> resetWifi(String deviceCode, ResetWifiRequest request) {
		ensureDeviceExists(deviceCode);
		if (!Boolean.TRUE.equals(request.confirm())) {
			throw badRequest("Bạn cần xác nhận trước khi reset Wi-Fi.");
		}
		long commandId = repository.createCommand(deviceCode, "reset_wifi", null);
		repository.createDeviceLog(deviceCode, "command", "Yêu cầu reset Wi-Fi");
		commandSender.sendCommand(deviceCode, commandId, "reset_wifi", null);
		Map<String, Object> command = repository.findCommand(deviceCode, commandId).orElseThrow(() -> notFound("Không tìm thấy lệnh."));
		return Map.of(
				"message", "Đã tạo lệnh reset Wi-Fi",
				"command_id", command.get("command_id"),
				"device_code", command.get("device_code"),
				"command_type", command.get("command_type"),
				"status", command.get("status"),
				"created_at", command.get("created_at")
		);
	}

	public List<Map<String, Object>> listCommands(String deviceCode, String status) {
		ensureDeviceExists(deviceCode);
		if (status != null && !status.isBlank()) {
			requireOneOf(status, COMMAND_FILTER_STATUSES, "status không hợp lệ.");
		}
		return repository.listCommands(deviceCode, status);
	}

	@Transactional
	public Map<String, Object> claimNextCommand(String deviceCode) {
		ensureDeviceExists(deviceCode);
		return repository.claimNextPendingCommand(deviceCode).orElse(null);
	}

	@Transactional
	public Map<String, Object> updateCommandStatus(String deviceCode, long commandId, CommandStatusRequest request) {
		ensureDeviceExists(deviceCode);
		requireOneOf(request.status(), COMMAND_STATUSES, "status không hợp lệ.");
		Map<String, Object> command = repository.findCommand(deviceCode, commandId).orElseThrow(() -> notFound("Không tìm thấy lệnh."));
		repository.updateCommandStatus(deviceCode, commandId, request.status());
		repository.createDeviceLog(deviceCode, "command", defaultMessage(request.message(), "Cập nhật trạng thái lệnh: " + request.status()));
		if ("feed_now".equals(command.get("command_type")) && Set.of("success", "failed").contains(request.status())) {
			String message = defaultMessage(request.message(), "Thiết bị đã xử lý lệnh cho ăn");
			repository.createFeedingLog(deviceCode, commandId, "manual", command.get("portion_size").toString(), request.status(), message);
		}
		return Map.of(
				"message", "Cập nhật trạng thái lệnh thành công",
				"command_id", commandId,
				"status", request.status()
		);
	}

	private void ensureDeviceExists(String deviceCode) {
		if (!repository.deviceExists(deviceCode)) {
			throw notFound("Không tìm thấy thiết bị.");
		}
	}

	private LocalTime requireFeedTime(String feedTime) {
		if (feedTime == null || feedTime.isBlank()) {
			throw badRequest("feed_time là bắt buộc.");
		}
		try {
			return LocalTime.parse(feedTime, FEED_TIME_FORMAT);
		} catch (DateTimeParseException exception) {
			throw badRequest("feed_time phải có định dạng HH:mm.");
		}
	}

	private String requirePortionSize(String portionSize) {
		if (portionSize == null || portionSize.isBlank()) {
			throw badRequest("portion_size là bắt buộc.");
		}
		requireOneOf(portionSize, PORTION_SIZES, "portion_size phải là small, medium, hoặc large.");
		return portionSize;
	}

	private void requireOneOf(String value, Set<String> allowed, String message) {
		if (value == null || !allowed.contains(value)) {
			throw badRequest(message);
		}
	}

	private LocalTime toLocalTime(Object value) {
		if (value instanceof LocalTime localTime) {
			return localTime;
		}
		return LocalTime.parse(value.toString());
	}

	private List<Map<String, Object>> formatScheduleRows(List<Map<String, Object>> rows) {
		return rows.stream().map(this::formatSchedule).toList();
	}

	private Map<String, Object> formatSchedule(Map<String, Object> row) {
		Map<String, Object> formatted = new LinkedHashMap<>(row);
		formatted.put("feed_time", toLocalTime(row.get("feed_time")).format(FEED_TIME_FORMAT));
		return formatted;
	}

	private int normalizeLimit(int limit, int defaultLimit, int maxLimit) {
		if (limit <= 0) {
			return defaultLimit;
		}
		return Math.min(limit, maxLimit);
	}

	private String defaultMessage(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}
}
