# Security Policy

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use GitHub's private reporting instead: go to the **Security** tab of this repository and click **Report a vulnerability**. Include the steps to reproduce and the affected sample (Java 8 Boot, Java 8 XML, or Java 17/21).

You should get a first reply within 7 days.

## Scope

This repository contains **sample code and a local demo setup**. Keep in mind:

- The secrets and passwords in `keycloak/realm-export.json` are public demo values. They are not a vulnerability. Never reuse them.
- The `dev-cli` client (password grant) exists only for local testing and must never be created in a real environment.
- `keycloak/docker-compose.yml` runs Keycloak in development mode. It is not a production setup.

Reports about the security logic itself are very welcome, for example: token validation, role mapping, access rules, error handling, or advice in the docs that would lead to an insecure setup.

## Supported versions

Only the latest commit on `main` is maintained.

The Java 8 samples use Spring Boot 2.7 and Spring Security 5.8, which no longer get free security updates from the Spring team. They are here to help teams migrate. New services should start from the Java 17/21 sample.
