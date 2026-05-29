# SmartHome

Java 21 Spring Boot API for a simple smart home device registry.

## Run

```powershell
.\gradlew.bat bootRun
```

Open the Swagger UI at:

```text
http://localhost:8080/swagger.html
```

Opening the app root also redirects to Swagger UI:

```text
http://localhost:8080
```

Useful endpoints:

- `GET /api/devices`
- `GET /api/devices/{id}`
- `POST /api/devices`

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
