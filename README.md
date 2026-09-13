# Chitram Backend

Chitram's backend is a Spring Boot REST and WebSocket service. It owns authentication, authorization, PostgreSQL access, Supabase Storage integration, feed queries, recommendations, moderation, reports, and admin operations.

## Repositories and Deployment

- Parent repository: `https://github.com/sairahul5/Chitram`
- Backend repository: `https://github.com/sairahul5/chitram-backend`
- Frontend repository: `https://github.com/sairahul5/chitram-frontend`
- Deployed backend: `https://chitram-backend-og9p.onrender.com`
- Deployed frontend: `https://chitram-frontend.vercel.app`

The parent repository contains `backend` and `frontend` as Git submodules. Never create a new repository for a component change.

## Requirements

- Java 17 or later
- Maven 3.9 or later
- PostgreSQL-compatible Supabase Session Pooler connection

## Configuration

Copy `.env.example` to `.env` inside `backend/` and provide the deployment secrets locally or in Render environment variables:

```env
SUPABASE_DB_URL=jdbc:postgresql://<POOLER_HOST>:5432/postgres?user=postgres.<PROJECT_REF>&password=<PASSWORD>&sslmode=require
GOOGLE_CLIENT_ID=<GOOGLE_CLIENT_ID>
GOOGLE_CLIENT_SECRET=<GOOGLE_CLIENT_SECRET>
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
FRONTEND_URL=http://localhost:3000,https://chitram-frontend.vercel.app
```

Supabase PostgreSQL stores application data. Supabase Storage stores uploaded images. The frontend never connects directly to either service; all access goes through this backend. Never commit `.env` or expose the service-role key.

## Run and Validate

Run from `backend/` so Spring Boot imports `.env`:

```bash
mvn spring-boot:run
mvn test
```

On startup the application verifies the database connection and creates/repairs only the small application-support tables required by the current features. Hibernate schema generation and Flyway are disabled.

## Authentication and Sessions

Google OAuth2 creates or updates a user and stores authenticated state in the `CHITRAM_SESSION` HTTP-only, secure session cookie. Protected APIs resolve the current user from the authenticated OAuth principal; IDs are never trusted from request bodies.

## Current Feature Areas

- Public feed: chronological feed, search, categories, pagination, likes, saves, uploads, and pin editing/deletion.
- Recommendations: optional personalized ranking. Admins can enable or disable it; disabled mode uses chronological feed and skips recommendation writes.
- Admin panel: real dashboard metrics, real database table overview, recent activity, user search/status/delete, pin moderation, reports, categories, platform settings, recommendation switch, and activity log.
- Moderation: pins support `PENDING`, `APPROVED`, `REJECTED`, and `HIDDEN`; only approved pins enter public and personalized feeds.
- Reports: the current admin report foundation supports pin reports and report status review. AI moderation is not configured.
- WebSockets: admin dashboard, users, new images, and deleted-image events use STOMP/SockJS.

## Important API Groups

- `/api/auth/*`: session status and logout; OAuth routes are `/oauth2/**` and `/login/**`.
- `/api/visual-items/*`: public feed plus authenticated upload/edit/delete operations.
- `/api/visual-items/random/tech`: returns one random approved tech image. Pass `excludeId` from the previous response, for example `/api/visual-items/random/tech?excludeId=42`, to ensure refreshes return a different image when another tech image exists.
- `/api/recommendations/*`: status, personalized feed, and recommendation interactions.
- `/api/admin/*`: admin dashboard, database/settings, users, categories, reports, moderation, and activity.
- `/api/user/*`: profiles, search, saved pins, follows, and user operations.

Admin endpoints require an authenticated account with the admin role. Public feed and session-status endpoints remain available according to their individual security rules.

## Database Support Tables

At startup the service ensures support for saved pins, pin likes, user interactions/interests, app settings, account status, moderation status, categories, reports, and admin activity logs. Existing data is preserved through idempotent `IF NOT EXISTS` statements and column repairs.

The admin database section reads actual PostgreSQL tables from `information_schema` and calculates exact row counts. It does not show planned placeholder tables.

## Deployment Notes

Render must define the backend environment variables above. Vercel must define:

```env
NEXT_PUBLIC_API_URL=https://chitram-backend-og9p.onrender.com/api
```

The backend CORS allowlist must include the Vercel origin. Deploy backend changes to Render before testing new backend APIs from the deployed frontend.
