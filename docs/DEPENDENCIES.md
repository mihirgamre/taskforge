# Dependencies

Selections were checked on 2026-08-09 against official project release pages or package registries. Local verification was completed on 2026-08-14.

| Area | Selected Version | Rationale |
| --- | --- | --- |
| Java | 25 LTS | Matches spec baseline and current LTS target for the backend. |
| Spring Boot | 4.1.0 | Current supported Spring Boot 4.1 stable release. |
| Maven | 3.9.15 | Current recommended Maven 3.x release; Maven 4 remains preview/RC. |
| PostgreSQL | 18.4 | Current PostgreSQL 18 patch release. |
| Apache Kafka | 4.3.1 | Current supported Kafka 4.x bugfix release with KRaft support. |
| Redis | 8.8.1 | Current stable Redis 8 release line from Redis downloads. |
| Node.js | 24.x LTS | Active LTS line; local machine currently has Node 24.15.0. |
| React | 19.2.7 | Latest stable React 19 patch line. |
| React Router | 7.18.2 | Selected after `npm audit` flagged vulnerabilities in 7.8.0. |
| Vite React plugin | 6.0.4 | Current plugin line with Vite 8 peer dependency support. |
| TypeScript | 6.0.3 | Latest stable TypeScript version inside the currently supported `typescript-eslint` range; TypeScript 7.0.2 was rejected because lint tooling does not support it yet. |
| Vite | 8.1.5 | Current stable supported Vite release. |
| Vitest | 4.1.10 | Current stable Vitest line with Vite 8 support. |
| Playwright | 1.62.1 | Selected after audit flagged vulnerable versions below 1.55.1. |
| Docker | 29.7.2 observed locally | Compose specification is used instead of a pinned legacy compose file version. |
| Flyway | Managed by Spring Boot 4.1.0 | Keeps Flyway aligned with Spring Boot dependency management. |
| JUnit | Managed by Spring Boot 4.1.0 | Spring Boot 4.1 manages JUnit 6.x compatible test stack. |
| Testcontainers | 1.21.4 | Current Maven Central version for the Java coordinates used here (`org.testcontainers:junit-jupiter`, `postgresql`, `kafka`). |

## Local Verification Notes

- The local shell has Java 25.0.4.
- The Maven Wrapper is vendored and was used for backend verification.
- PowerShell script execution blocks `npm`; use `npm.cmd` on Windows.
