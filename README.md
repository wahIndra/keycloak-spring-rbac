# Keycloak RBAC for Spring (Java 8, 17, 21)

Secure-by-default examples of role-based access control for Spring APIs with Keycloak, in three flavors:

| Folder | Java | Stack | Port |
| --- | --- | --- | --- |
| [`java17-21-spring-boot`](java17-21-spring-boot) | 17 or 21 | Spring Boot 4, Spring Security 7 | 8081 |
| [`java8-spring-boot`](java8-spring-boot) | 8 | Spring Boot 2.7, Spring Security 5.8 | 8082 |
| [`java8-spring-xml`](java8-spring-xml) | 8 | Spring MVC 5.3 + XML config, Spring Security 5.8, no Spring Boot | 8083 |

The full guide (concepts, diagrams, security model, Keycloak setup, production hardening, migration, troubleshooting, checklist) is in [`docs/keycloak-rbac-spring-best-practices.md`](docs/keycloak-rbac-spring-best-practices.md). Its code blocks are copied from these samples.

## What the samples show

- **Full token validation**: RS256 signature, `exp` (required), `iss`, `aud`, and `typ` (access tokens only).
- **Client roles** (`resource_access.<client>.roles`) turned into Spring roles (`ROLE_...`).
- **Deny by default**: rules include the HTTP method, and any path not listed returns 401/403.
- **Ownership checks** with `@PreAuthorize`: a partner can only submit its own data, based on a claim Keycloak sets.
- **Input validation** on every request body and path variable.
- **Safe errors**: 400/401/403 as RFC 9457 `application/problem+json`, with no stack traces or exception messages.
- **Audit logging** of every 401/403 (who, which client, what), without ever logging tokens.
- **Fail closed**: the apps refuse to start without their Keycloak settings.
- **Hardened demo realm**: one client per caller, *Full scope allowed* off, audience mappers, brute force detection, password policy, events.
- **Tests** for every rule, plus CI and Dependabot.

## Project layout

```
.
├── docs/                       the guide
├── keycloak/
│   ├── docker-compose.yml      local Keycloak 26 with the demo realm
│   ├── realm-export.json       realm "myrealm": clients, roles, groups, demo users
│   ├── get-token.sh            get a token from the terminal (asks for secrets, keeps them out of history)
│   └── .env.example            admin credentials template for docker compose
├── java17-21-spring-boot/      leads-api on Java 17/21
├── java8-spring-boot/          leads-api on Java 8 + Spring Boot
├── java8-spring-xml/           orders-api on Java 8 + XML (replaces the old Keycloak adapter)
├── .github/                    CI and Dependabot
└── SECURITY.md                 how to report a vulnerability
```

## Requirements

- Docker (for Keycloak)
- JDK 17 or 21 for `java17-21-spring-boot`, JDK 8 for the two Java 8 samples
- Maven 3.6.3+
- `curl` and `jq` for the examples below

## Quick start

**1. Start Keycloak**

```bash
cd keycloak
cp .env.example .env        # then set your own admin password in .env
docker compose up -d
```

Admin console: http://localhost:8080 (only reachable from your own machine). The realm `myrealm` is imported on startup.

**2. Run a sample** (in another terminal)

The Boot samples need their Keycloak settings. The `local` profile fills them in for the demo Keycloak:

```bash
# Java 17 / 21
cd java17-21-spring-boot && mvn spring-boot:run -Dspring-boot.run.profiles=local   # http://localhost:8081

# Java 8 + Spring Boot
cd java8-spring-boot && mvn spring-boot:run -Dspring-boot.run.profiles=local       # http://localhost:8082

# Java 8 + XML (reads src/main/resources/security.properties)
cd java8-spring-xml && mvn jetty:run                                               # http://localhost:8083
```

In any other environment, set `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_AUDIENCE`, and `KEYCLOAK_CLIENT_ID` instead (see Configuration).

**3. Run the tests** (no Keycloak needed), inside each sample folder:

```bash
mvn verify
```

## Demo accounts

All of these exist in `keycloak/realm-export.json`. They are **public demo values for your own machine only**.

| Account | Type | Secret / password | Gets roles | Notes |
| --- | --- | --- | --- | --- |
| `partner-xyz` | Client (service account) | `partner-xyz-secret` | `B2B_LEADS_SUBMIT`, `LEADS_READ` on `leads-api` | Token also has `partner_id = XYZ001` |
| `billing-service` | Client (service account) | `billing-service-secret` | `LEADS_READ` on `leads-api` | |
| `orders-client` | Client (service account) | `orders-client-secret` | `ORDERS_WRITE`, `PAYMENTS_WRITE` on `orders-api` | Token is for `orders-api`, not `leads-api` |
| `alice` | User, group `cs-agent` | `alice-local-demo-1` | `LEADS_READ` | |
| `bob` | User, group `cs-supervisor` | `bob-local-demo-12` | `LEADS_OPERATOR` (= `LEADS_READ` + `LEADS_WRITE`) | |
| `carol` | User, group `mobile-users` | `carol-local-demo-1` | `MOBILE_ACCESS` | |

Real apps log users in through `web-portal` (authorization code + PKCE). For terminal testing, `get-token.sh user` uses the `dev-cli` client, which has the password grant and exists **only in this local demo realm**.

## Try it

Run these from the repository root. `get-token.sh` asks for the secret or password. The examples call the Java 17/21 sample on port 8081. For the Java 8 Boot sample, use port 8082.

```bash
chmod +x keycloak/get-token.sh
API=http://localhost:8081
LEAD='{"name":"Jane","phone":"0800123456","product":"loan"}'
```

**Partner (client credentials)**

```bash
TOKEN=$(keycloak/get-token.sh client partner-xyz)

# 200: own partner id
curl -i -X POST $API/b2b/partners/XYZ001/leads \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$LEAD"

# 403: another partner's id (blocked by @PreAuthorize)
curl -i -X POST $API/b2b/partners/OTHER1/leads \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$LEAD"

# 400: invalid input (problem+json body)
curl -i -X POST $API/b2b/partners/XYZ001/leads \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":""}'

# 200: partner also has LEADS_READ
curl -i $API/leads/1 -H "Authorization: Bearer $TOKEN"

# 403: path not listed in the rules (deny by default)
curl -i $API/internal/ping -H "Authorization: Bearer $TOKEN"
```

**Users**

```bash
# alice (LEADS_READ): read works, write is blocked
TOKEN=$(keycloak/get-token.sh user alice)
curl -i $API/leads/1 -H "Authorization: Bearer $TOKEN"                     # 200
curl -i -X POST $API/leads -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d "$LEAD"                             # 403

# bob (LEADS_OPERATOR): read and write
TOKEN=$(keycloak/get-token.sh user bob)
curl -i -X POST $API/leads -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d "$LEAD"                             # 201

# carol (MOBILE_ACCESS): Java 17/21 sample only
TOKEN=$(keycloak/get-token.sh user carol)
curl -i $API/mobile/me -H "Authorization: Bearer $TOKEN"                   # 200
```

**Bad tokens**

```bash
# 401: no token
curl -i $API/leads/1

# 401: token for orders-api, not leads-api (aud check)
TOKEN=$(keycloak/get-token.sh client orders-client)
curl -i $API/leads/1 -H "Authorization: Bearer $TOKEN"
```

**XML sample (orders-api, port 8083)**

```bash
TOKEN=$(keycloak/get-token.sh client orders-client)
curl -i -X POST http://localhost:8083/order -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"item":"book"}'                   # 200
curl -i -X POST http://localhost:8083/payment -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"amount":12.50}'                  # 200
curl -i http://localhost:8083/status/O-1 -H "Authorization: Bearer $TOKEN"   # 200
curl -i http://localhost:8083/internal/ping -H "Authorization: Bearer $TOKEN" # 403

# 401: partner token is for leads-api, not orders-api
TOKEN=$(keycloak/get-token.sh client partner-xyz)
curl -i -X POST http://localhost:8083/order -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"item":"book"}'
```

## Endpoints and roles

**leads-api** (`java17-21-spring-boot`, `java8-spring-boot`)

| Method | Path | Needs |
| --- | --- | --- |
| GET | `/actuator/health` | Nothing (public, no details) |
| GET | `/leads/{id}` | `LEADS_READ` or `LEADS_OPERATOR` |
| POST | `/leads` | `LEADS_WRITE` or `LEADS_OPERATOR` |
| POST | `/b2b/partners/{partnerId}/leads` | `B2B_LEADS_SUBMIT`, and `partnerId` must equal the token's `partner_id` |
| GET | `/mobile/me` | `MOBILE_ACCESS` (Java 17/21 sample only) |
| GET | `/internal/ping` | Always denied (not listed in the rules) |

**orders-api** (`java8-spring-xml`)

| Method | Path | Needs |
| --- | --- | --- |
| GET | `/health` | Nothing (public) |
| POST | `/order` | `ORDERS_WRITE` |
| POST | `/payment` | `PAYMENTS_WRITE` |
| GET | `/status/{id}` | `ORDERS_WRITE` or `PAYMENTS_WRITE` |
| GET | `/internal/ping` | Always denied (not listed in the rules) |

## Configuration

The Boot samples read these environment variables. The required ones have no defaults on purpose: the app won't start without them.

| Variable | Required | Meaning |
| --- | --- | --- |
| `KEYCLOAK_ISSUER_URI` | Yes | Public realm URL. Must match the token's `iss` exactly |
| `KEYCLOAK_AUDIENCE` | Yes | This API's client name. Must be in the token's `aud` |
| `KEYCLOAK_CLIENT_ID` | Yes | Client whose roles are read from `resource_access` |
| `KEYCLOAK_JWK_SET_URI` | No | Internal URL for Keycloak's keys, if the API can't use the public one |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated browser origins (Java 17/21 sample). Empty = no browser access |

The XML sample reads `src/main/resources/security.properties`, which holds local values. In other environments, override them with JVM system properties, for example `-Dkeycloak.issuer-uri=https://sso.example.com/realms/myrealm`.

## Before you reuse this

- Demo secrets and passwords in `realm-export.json` are public. Never reuse them.
- `dev-cli` (password grant) and `start-dev` are for local use only.
- Read "Production hardening" in the guide before going live.
- Library versions were current when this was written. Dependabot keeps them updated. Spring Boot 2.7 and Spring Security 5.8 no longer get free security fixes, so use the Java 8 samples to migrate, then move to Java 17/21.

## Security

See [SECURITY.md](SECURITY.md) to report a vulnerability.
