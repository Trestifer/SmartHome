# SmartHome

Java 21 Spring Boot API for a Pet Feeder IoT backend.

## Run

```powershell
.\gradlew.bat bootRun
```

Open the Swagger UI at:

```text
http://localhost:8080/swagger-ui/index.html
```

Opening the app root also redirects to Swagger UI:

```text
http://localhost:8080
```

Useful endpoints:

- `GET /api/v1/devices/{device_code}`
- `PATCH /api/v1/devices/{device_code}/status`
- `POST /api/v1/devices/{device_code}/commands/feed-now`
- `GET /api/v1/devices/{device_code}/schedules`
- `POST /api/v1/devices/{device_code}/schedules`
- `PATCH /api/v1/devices/{device_code}/schedules/{schedule_id}`
- `DELETE /api/v1/devices/{device_code}/schedules/{schedule_id}`
- `GET /api/v1/devices/{device_code}/feeding-logs`
- `GET /api/v1/devices/{device_code}/device-logs`
- `POST /api/v1/devices/{device_code}/commands/reset-wifi`
- `GET /api/v1/devices/{device_code}/commands`
- `PATCH /api/v1/devices/{device_code}/commands/{command_id}`

## Supabase PostgreSQL

Copy the example properties file:

```text
src/main/resources/application.properties.example
```

Create the ignored local file:

```text
src/main/resources/application.properties
```

Then add the real password and run:

```powershell
.\gradlew.bat bootRun
```

Connection values:

- Host: `aws-1-ap-northeast-1.pooler.supabase.com`
- Port: `5432`
- Database: `postgres`
- User: `postgres.zsmcaaqwogoghnprfmmx`

## Azure Deploy

The GitHub Actions workflow builds this Gradle project with:

```powershell
.\gradlew.bat clean test bootJar
```

Set these Azure Web App application settings for the hosted app:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.zsmcaaqwogoghnprfmmx
SPRING_DATASOURCE_PASSWORD=your-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

GitHub also needs the publish profile secret:

```text
AzureAppService_PublishProfile_1234
```
