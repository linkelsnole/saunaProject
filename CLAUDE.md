# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A full-stack sauna control system: React frontend + Spring Boot backend communicating with a PLC over an HTTP-based Modbus API. The UI displays sensor readings (temperature, pressure, humidity) and allows control of fans, recuperators, and heating setpoints.

## Commands

### Frontend
```bash
npm run dev       # Vite dev server on :5173 (proxies /api → :8080)
npm run build     # Production build to /dist
npm run lint      # ESLint
```

### Backend
```bash
cd backend && bash start.sh         # Dev run (Spring Boot on :8080)
cd backend && mvn spring-boot:run   # Alternative dev run
cd backend && mvn clean package -DskipTests  # Full build (includes frontend build)
```

### Deploy to Production
```bash
./deploy.sh   # Builds JAR, SCPs to 212.164.86.237, restarts systemd service
```

## Architecture

**Data flow**: React → React Query → `fetchWithAuth()` (adds JWT) → Spring Boot (`/api/v1/*`) → Modbus HTTP API (212.164.86.237:4567)

### Frontend (`src/`)
- `api/` — All API integration: `typesApi.ts` (all DTOs), `deviceApi.ts` (endpoints), `hooks.ts` (mutation hooks), `useDashboard.ts` (query hook with 7s auto-refetch)
- `schema/` — Konva canvas schematic diagram showing pipes, pressure gauges, and temperatures
- `components/settings/` — Control widgets for fan, temperature, and recuperator settings
- `contexts/` — `AuthContext.tsx` (JWT auth state), `SettingsSheetContext.tsx` (mobile drawer)
- `shared/ui/` — shadcn/ui component library

**Path alias**: `@/` → `src/`

**Auth**: JWT stored in `localStorage` (`auth_token`, `auth_username`, `auth_role`). 401 responses redirect to `/login`.

**State**: React Query for server state, React Context for auth/UI state. Redux is set up but unused.

### Backend (`backend/src/main/java/com/example/modbus_api/`)
- `controller/DeviceController.java` — All device endpoints (`/api/v1/devices/*`)
- `service/DeviceService.java` — Reads Modbus registers and assembles `DashboardData`
- `service/SettingsService.java` — Writes settings back to Modbus registers
- `service/ModbusConstants.java` — All Modbus register addresses and scale factors
- `security/` — JWT filter, Spring Security config, `AppUser` JPA entity with SQLite
- `dto/` — Request/response DTOs mirrored in `src/api/typesApi.ts`

**Modbus scaling**: All numeric values are stored ×10 in registers (e.g., 90.1°C = 901). The backend converts on read/write; the frontend receives natural units.

**Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_READONLY`, `ROLE_MGM` — enforced via `@PreAuthorize` on controller methods.

**Build integration**: `pom.xml` uses `frontend-maven-plugin` to run `npm install` + `npm run build`, then copies `/dist` into `target/classes/static`. The resulting JAR serves the frontend as static resources.

## Key Config
- Backend config: `backend/src/main/resources/application.properties` (Modbus IP/port, SQLite path, JWT secret/expiration)
- Vite proxy: `vite.config.ts` proxies `/api` to `http://localhost:8080`
- shadcn config: `components.json`
