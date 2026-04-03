---
id: contributing
title: Contributing
---

# Contributing to Easygram

Thank you for your interest in contributing! This guide covers everything you need to get started.

## Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Java | 17 |
| Maven | 3.8+ |
| Git | any recent version |

## Getting the Code

```bash
# Fork the repo on GitHub, then clone your fork
git clone https://github.com/<your-username>/easygram.git
cd easygram

# Add the upstream remote
git remote add upstream https://github.com/oson-code/easygram.git
```

## Building the Project

```bash
# Full build (compiles all modules, runs tests)
mvn install

# Skip tests for a faster build during development
mvn install -DskipTests

# Build a specific module
mvn install -pl core-api -am
```

## Project Structure

```
easygram/
├── core-api/               # Public API: annotations, reply types, interfaces
├── core/                   # Core implementation: dispatcher, filters, auto-config
├── core-i18n/              # i18n support: LocalizedReply, LocalizedTemplate
├── core-chatstate/         # Chat state persistence interfaces
├── core-observability/     # Micrometer metrics and actuator endpoint
├── longpolling/            # Long-polling transport
├── webhook/                # Webhook transport
├── messaging-api/          # Unified broker module: publisher SPI + Kafka + RabbitMQ (producers & consumers)
├── spring-boot-starter/    # Aggregator starter (includes all transports)
└── samples/                # Example bots
```

**Dependency direction:** `core-api` has no framework dependencies. Everything else depends on `core-api`. `core` depends on `core-api`. Transport/messaging modules depend on `core`.

## Running the Samples

```bash
cd samples/<sample-name>
export BOT_TOKEN="your_telegram_bot_token"
mvn spring-boot:run
```

## Code Style

- **Java 17** — use records, sealed classes, text blocks where appropriate
- **Lombok** — use `@Slf4j`, `@RequiredArgsConstructor`, etc. Avoid `@Data` on mutable types
- **Immutability** — reply types are immutable; wither methods (`withMarkup`, etc.) return new instances
- **Builder pattern** — public API types expose a static inner `Builder` class with a `builder()` factory and `build()` terminal method
- **Template tokens** — use `#{0}`, `#{1}`, ... (0-based) for positional substitution in `PlainTextTemplate` and `LocalizedTemplate`
- **Javadoc** — all public types and methods must have Javadoc; include `@since` with the version introduced
- **No wildcard imports** — configure your IDE to expand them

## Reporting Bugs

1. Search [existing issues](https://github.com/oson-code/easygram/issues) first
2. Open a new issue with the **Bug** label
3. Include:
   - Easygram version
   - Java and Spring Boot versions
   - Minimal reproducer
   - Expected vs actual behaviour
   - Full stack trace if applicable

## Requesting Features

1. Search [existing issues](https://github.com/oson-code/easygram/issues) for similar requests
2. Open a new issue with the **Enhancement** label
3. Describe the use case and why it cannot be solved with existing APIs

## Submitting a Pull Request

1. **Create a branch** from `main`:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make your changes** — keep them focused on a single concern

3. **Build and verify:**
   ```bash
   mvn install
   ```

4. **Commit** following the [conventions below](#commit-message-conventions)

5. **Push and open a PR** against `main`:
   ```bash
   git push origin feature/my-feature
   ```

6. In your PR description, explain what changed, reference any related issue (`Fixes #123`), and list breaking changes.

## Commit Message Conventions

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short summary>
```

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that is neither a fix nor a feature |
| `test` | Adding or updating tests |
| `chore` | Build, CI, or tooling changes |

**Examples:**

```
feat(core-api): add builder pattern to PlainReply
fix(webhook): validate secret-token header before deserialization
docs(transports): add jprq local testing guide to webhook-guide
```

## License

By contributing you agree that your contributions will be licensed under the [MIT License](https://github.com/oson-code/easygram/blob/main/LICENSE).
