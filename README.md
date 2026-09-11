# Chitram Backend

Minimal Spring Boot foundation for connecting Chitram to an existing Supabase PostgreSQL database.

## Requirements

- Java 17 or later
- Maven 3.9 or later
- A Supabase JDBC Session Pooler connection string

## Configure the database connection

Copy `.env.example` to `.env` and replace the placeholders with your real Supabase URL and Google OAuth secret. `.env` is ignored by Git and is imported automatically when Spring Boot starts from `backend/`. Never put these values in `application.properties`.

macOS or Linux:

```bash
cp .env.example .env
```

The application expects the Session Pooler URL on port `5432`. Replace the placeholders with the values from Supabase.

If running from IntelliJ IDEA, set the run configuration working directory to `backend/`. The application will load `backend/.env` automatically. Alternatively, add the four `.env` values under **Run Configuration > Environment variables**.

## Start the backend

From `backend/`:

```bash
mvn spring-boot:run
```

The same command works after `.env` has been filled in. Do not run with the placeholder values.

On startup, the application runs `SELECT 1`. A successful connection prints:

```text
Chitram database connection successful
```

A failed connection stops startup and includes the database error message without logging the configured JDBC URL.

Hibernate schema changes are disabled with `spring.jpa.hibernate.ddl-auto=none`; this project does not create or modify database tables.
