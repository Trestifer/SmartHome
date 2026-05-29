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

Useful endpoints:

- `GET /api/devices`
- `GET /api/devices/{id}`
- `POST /api/devices`

## Supabase PostgreSQL Example

The sample Supabase connection settings are in:

```text
src/main/resources/application-supabase-example.properties
```

Set the database password as an environment variable before running with that profile:

```powershell
$env:SUPABASE_DB_PASSWORD = "your-password"
.\gradlew.bat bootRun --args='--spring.profiles.active=supabase-example'
```

Connection values:

- Host: `aws-1-ap-northeast-1.pooler.supabase.com`
- Port: `5432`
- Database: `postgres`
- User: `postgres.zsmcaaqwogoghnprfmmx`
