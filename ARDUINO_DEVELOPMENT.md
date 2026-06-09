# Arduino Development

This backend is designed for an ESP32 or Arduino-compatible pet feeder device that polls the SmartHome API, executes one command at a time, and reports the result.

## Device Flow

1. Update device status when the board boots.
2. Poll the API for the next pending command.
3. Execute the command locally.
4. Report `success` or `failed`.
5. Repeat every few seconds.

## API Endpoints

Use your deployed Azure base URL:

```text
https://your-azure-app.azurewebsites.net
```

Replace `PET001` with the real `device_code`.

```text
PATCH /api/v1/devices/PET001/status
POST /api/v1/devices/PET001/commands/next
PATCH /api/v1/devices/PET001/commands/{command_id}
```

`POST /commands/next` returns `204 No Content` when there is no work. When a command exists, the backend marks it `sent` and returns:

```json
{
  "command_id": 12,
  "device_code": "PET001",
  "command_type": "feed_now",
  "portion_size": "small",
  "status": "sent",
  "created_at": "2026-06-04T10:00:00Z",
  "sent_at": "2026-06-04T10:00:02Z",
  "completed_at": null
}
```

## Command Types

```text
feed_now
reset_wifi
```

`feed_now` uses `portion_size`:

```text
small
medium
large
```

## ESP32 Sketch Skeleton

Install these Arduino libraries:

```text
ArduinoJson
HTTPClient
WiFi
```

```cpp
#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <WiFi.h>

const char* WIFI_SSID = "your-wifi";
const char* WIFI_PASSWORD = "your-password";
const char* API_BASE_URL = "https://your-azure-app.azurewebsites.net";
const char* DEVICE_CODE = "PET001";

const int MOTOR_PIN = 18;

void setup() {
  Serial.begin(115200);
  pinMode(MOTOR_PIN, OUTPUT);
  connectWifi();
  updateDeviceStatus("online", "Device booted");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWifi();
  }

  pollAndExecuteCommand();
  delay(5000);
}

void connectWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void pollAndExecuteCommand() {
  HTTPClient http;
  String url = String(API_BASE_URL) + "/api/v1/devices/" + DEVICE_CODE + "/commands/next";
  http.begin(url);
  int statusCode = http.POST("");

  if (statusCode == 204) {
    http.end();
    return;
  }

  if (statusCode != 200) {
    http.end();
    return;
  }

  String payload = http.getString();
  http.end();

  JsonDocument doc;
  DeserializationError error = deserializeJson(doc, payload);
  if (error) {
    return;
  }

  long commandId = doc["command_id"];
  const char* commandType = doc["command_type"];
  const char* portionSize = doc["portion_size"] | "";

  bool ok = executeCommand(commandType, portionSize);
  reportCommandStatus(commandId, ok ? "success" : "failed", ok ? "Command executed" : "Command failed");
}

bool executeCommand(const char* commandType, const char* portionSize) {
  if (strcmp(commandType, "feed_now") == 0) {
    dispenseFood(portionSize);
    return true;
  }

  if (strcmp(commandType, "reset_wifi") == 0) {
    resetWifiSettings();
    return true;
  }

  return false;
}

void dispenseFood(const char* portionSize) {
  int durationMs = 800;

  if (strcmp(portionSize, "medium") == 0) {
    durationMs = 1400;
  } else if (strcmp(portionSize, "large") == 0) {
    durationMs = 2200;
  }

  digitalWrite(MOTOR_PIN, HIGH);
  delay(durationMs);
  digitalWrite(MOTOR_PIN, LOW);
}

void resetWifiSettings() {
  WiFi.disconnect(true, true);
  delay(1000);
  ESP.restart();
}

void updateDeviceStatus(const char* status, const char* message) {
  JsonDocument doc;
  doc["status"] = status;
  doc["message"] = message;

  String body;
  serializeJson(doc, body);

  HTTPClient http;
  String url = String(API_BASE_URL) + "/api/v1/devices/" + DEVICE_CODE + "/status";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.PATCH(body);
  http.end();
}

void reportCommandStatus(long commandId, const char* status, const char* message) {
  JsonDocument doc;
  doc["status"] = status;
  doc["message"] = message;

  String body;
  serializeJson(doc, body);

  HTTPClient http;
  String url = String(API_BASE_URL) + "/api/v1/devices/" + DEVICE_CODE + "/commands/" + commandId;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.PATCH(body);
  http.end();
}
```

## Hardware Notes

Do not drive a motor directly from an ESP32 GPIO pin. Use a motor driver, relay module, or MOSFET circuit with a separate motor power supply and a shared ground. Add a flyback diode if your driver module does not already include one.

Tune these values for the real feeder mechanism:

```text
small  -> 800 ms
medium -> 1400 ms
large  -> 2200 ms
```
