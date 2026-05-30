package com.trestifer.smarthome.petfeeder;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PetFeederRepository {

	private final JdbcClient jdbc;
	private final JdbcTemplate jdbcTemplate;

	public PetFeederRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
		this.jdbc = jdbc;
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Map<String, Object>> findDevice(String deviceCode) {
		return firstRow(jdbc.sql("""
				SELECT device_code, device_name, status, last_seen, created_at
				FROM devices
				WHERE device_code = :deviceCode
				""")
				.param("deviceCode", deviceCode)
				.query()
				.listOfRows());
	}

	public boolean deviceExists(String deviceCode) {
		Integer count = jdbc.sql("SELECT COUNT(*) FROM devices WHERE device_code = :deviceCode")
				.param("deviceCode", deviceCode)
				.query(Integer.class)
				.single();
		return count > 0;
	}

	public void updateDeviceStatus(String deviceCode, String status) {
		jdbc.sql("""
				UPDATE devices
				SET status = :status, last_seen = NOW()
				WHERE device_code = :deviceCode
				""")
				.param("status", status)
				.param("deviceCode", deviceCode)
				.update();
	}

	public long createCommand(String deviceCode, String commandType, String portionSize) {
		return jdbc.sql("""
				INSERT INTO device_commands (device_code, command_type, portion_size, status)
				VALUES (:deviceCode, :commandType, :portionSize, 'pending')
				RETURNING command_id
				""")
				.param("deviceCode", deviceCode)
				.param("commandType", commandType)
				.param("portionSize", portionSize)
				.query(Long.class)
				.single();
	}

	public Optional<Map<String, Object>> findCommand(String deviceCode, long commandId) {
		return firstRow(jdbc.sql("""
				SELECT command_id, device_code, command_type, portion_size, status, created_at, sent_at, completed_at
				FROM device_commands
				WHERE device_code = :deviceCode AND command_id = :commandId
				""")
				.param("deviceCode", deviceCode)
				.param("commandId", commandId)
				.query()
				.listOfRows());
	}

	public void updateCommandStatus(String deviceCode, long commandId, String status) {
		jdbc.sql("""
				UPDATE device_commands
				SET status = :status,
				    sent_at = CASE WHEN :status = 'sent' THEN NOW() ELSE sent_at END,
				    completed_at = CASE WHEN :status IN ('success', 'failed') THEN NOW() ELSE completed_at END
				WHERE device_code = :deviceCode AND command_id = :commandId
				""")
				.param("status", status)
				.param("deviceCode", deviceCode)
				.param("commandId", commandId)
				.update();
	}

	public List<Map<String, Object>> listCommands(String deviceCode, String status) {
		if (status == null || status.isBlank()) {
			return jdbc.sql("""
					SELECT command_id, device_code, command_type, portion_size, status, created_at, sent_at, completed_at
					FROM device_commands
					WHERE device_code = :deviceCode
					ORDER BY created_at DESC, command_id DESC
					""")
					.param("deviceCode", deviceCode)
					.query()
					.listOfRows();
		}
		return jdbc.sql("""
				SELECT command_id, device_code, command_type, portion_size, status, created_at, sent_at, completed_at
				FROM device_commands
				WHERE device_code = :deviceCode AND status = :status
				ORDER BY created_at DESC, command_id DESC
				""")
				.param("deviceCode", deviceCode)
				.param("status", status)
				.query()
				.listOfRows();
	}

	public List<Map<String, Object>> listSchedules(String deviceCode) {
		return jdbc.sql("""
				SELECT schedule_id, device_code, feed_time, portion_size, repeat_type, is_active, created_at, updated_at
				FROM feeding_schedules
				WHERE device_code = :deviceCode
				ORDER BY feed_time, schedule_id
				""")
				.param("deviceCode", deviceCode)
				.query()
				.listOfRows();
	}

	public Optional<Map<String, Object>> findSchedule(String deviceCode, long scheduleId) {
		return firstRow(jdbc.sql("""
				SELECT schedule_id, device_code, feed_time, portion_size, repeat_type, is_active, created_at, updated_at
				FROM feeding_schedules
				WHERE device_code = :deviceCode AND schedule_id = :scheduleId
				""")
				.param("deviceCode", deviceCode)
				.param("scheduleId", scheduleId)
				.query()
				.listOfRows());
	}

	public long createSchedule(String deviceCode, LocalTime feedTime, String portionSize) {
		return jdbc.sql("""
				INSERT INTO feeding_schedules (device_code, feed_time, portion_size, repeat_type, is_active)
				VALUES (:deviceCode, :feedTime, :portionSize, 'daily', TRUE)
				RETURNING schedule_id
				""")
				.param("deviceCode", deviceCode)
				.param("feedTime", Time.valueOf(feedTime))
				.param("portionSize", portionSize)
				.query(Long.class)
				.single();
	}

	public void updateSchedule(long scheduleId, LocalTime feedTime, String portionSize, boolean active) {
		jdbc.sql("""
				UPDATE feeding_schedules
				SET feed_time = :feedTime,
				    portion_size = :portionSize,
				    is_active = :active,
				    updated_at = NOW()
				WHERE schedule_id = :scheduleId
				""")
				.param("feedTime", Time.valueOf(feedTime))
				.param("portionSize", portionSize)
				.param("active", active)
				.param("scheduleId", scheduleId)
				.update();
	}

	public void deleteSchedule(String deviceCode, long scheduleId) {
		jdbc.sql("DELETE FROM feeding_schedules WHERE device_code = :deviceCode AND schedule_id = :scheduleId")
				.param("deviceCode", deviceCode)
				.param("scheduleId", scheduleId)
				.update();
	}

	public boolean hasActiveScheduleWithinFiveMinutes(String deviceCode, LocalTime feedTime, Long ignoredScheduleId) {
		if (ignoredScheduleId == null) {
			String sql = """
					SELECT COUNT(*)
					FROM feeding_schedules
					WHERE device_code = ?
					  AND is_active = TRUE
					  AND ABS(EXTRACT(EPOCH FROM (feed_time - ?::time))) < 300
					""";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, deviceCode, Time.valueOf(feedTime));
			return count != null && count > 0;
		}
		String sql = """
				SELECT COUNT(*)
				FROM feeding_schedules
				WHERE device_code = ?
				  AND is_active = TRUE
				  AND schedule_id <> ?
				  AND ABS(EXTRACT(EPOCH FROM (feed_time - ?::time))) < 300
				""";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
				deviceCode,
				ignoredScheduleId,
				Time.valueOf(feedTime));
		return count != null && count > 0;
	}

	public List<Map<String, Object>> listFeedingLogs(String deviceCode, int limit) {
		return jdbc.sql("""
				SELECT log_id, device_code, schedule_id, command_id, feed_type, portion_size, status, message, fed_at
				FROM feeding_logs
				WHERE device_code = :deviceCode
				ORDER BY fed_at DESC, log_id DESC
				LIMIT :limit
				""")
				.param("deviceCode", deviceCode)
				.param("limit", limit)
				.query()
				.listOfRows();
	}

	public List<Map<String, Object>> listDeviceLogs(String deviceCode, int limit) {
		return jdbc.sql("""
				SELECT log_id, device_code, log_type, message, created_at
				FROM device_logs
				WHERE device_code = :deviceCode
				ORDER BY created_at DESC, log_id DESC
				LIMIT :limit
				""")
				.param("deviceCode", deviceCode)
				.param("limit", limit)
				.query()
				.listOfRows();
	}

	public void createDeviceLog(String deviceCode, String logType, String message) {
		jdbc.sql("""
				INSERT INTO device_logs (device_code, log_type, message)
				VALUES (:deviceCode, :logType, :message)
				""")
				.param("deviceCode", deviceCode)
				.param("logType", logType)
				.param("message", message)
				.update();
	}

	public void createFeedingLog(String deviceCode, Long commandId, String feedType, String portionSize, String status, String message) {
		jdbc.sql("""
				INSERT INTO feeding_logs (device_code, command_id, feed_type, portion_size, status, message)
				VALUES (:deviceCode, :commandId, :feedType, :portionSize, :status, :message)
				""")
				.param("deviceCode", deviceCode)
				.param("commandId", commandId)
				.param("feedType", feedType)
				.param("portionSize", portionSize)
				.param("status", status)
				.param("message", message)
				.update();
	}

	private Optional<Map<String, Object>> firstRow(List<Map<String, Object>> rows) {
		return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
	}
}
