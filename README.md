# easygram

[![Maven Central](https://img.shields.io/maven-central/v/uz.osoncode.easygram/spring-boot-starter)](https://central.sonatype.com/artifact/uz.osoncode.easygram/spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> A Spring Boot framework for building Telegram bots with annotation-driven routing, pluggable transports (long-polling, webhook, Kafka, RabbitMQ), and zero boilerplate.

---

## Table of Contents

- [Features](#-features)
- [Module Structure](#-module-structure)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Transport Selection](#-transport-selection)
- [Quick Start](#-quick-start)
- [Internationalisation (i18n)](#internationalisation-i18n)
- [Threading Model](#-threading-model)
- [Module Documentation](#-module-documentation)
- [Building from Source](#-building-from-source)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

- **Zero-config autoconfiguration** — a single property is all that's required to start
- **Four transports** — long-polling (default), webhook, Kafka consumer, RabbitMQ consumer; switch with one property
- **Annotation-driven routing** — `@BotCommand`, `@BotText`, `@BotCallbackQuery`, `@BotContact`, `@BotLocation`, `@BotDefaultHandler`, and more
- **Smart parameter injection** — inject `Update`, `User`, `Chat`, `CallbackQuery`, `TelegramClient`, `BotRequest`, `BotResponse`, or annotated scalars; wrap any type in `Optional<T>` for safe optional injection
- **Composable filter pipeline** — `BotFilter` to intercept every update before handlers (auth, logging, rate-limiting, …)
- **Chat state management** — `@BotChatState` + pluggable `BotChatStateService`; ships with a thread-safe in-memory default; annotation-driven transitions via `@BotForwardChatState` / `@BotClearChatState`
- **Handler priority** — `@BotOrder` for fine-grained control when multiple handlers match
- **Exception handling** — `@BotExceptionHandler` per exception type inside `@BotController`
- **Multiple return types** — `BotApiMethod<?>`, `Collection<BotApiMethod<?>>`, `Collection<Object>` (mixed), `String`, `LocalizedReply`, or `void`
- **Broker publishing** — publish every update to Kafka or RabbitMQ via a single dependency
- **Internationalisation (i18n)** — `core-i18n` provides locale resolution, `BotMessageSource`, `BotKeyboardFactory`, `@BotReplyButton`, `LocalizedReply` return type, and `Locale` injection out-of-the-box
- **Fully overridable** — every bean uses `@ConditionalOnMissingBean`; swap anything without forking

---

## 🗂️ Module Structure

```
easygram/
│
├── core-api/                   # Pure contracts: interfaces, annotations, models
│                               # (BotFilter, BotHandler, BotRequest, BotResponse,
│                               #  all handler/arg annotations, BotConfigurer, …)
│
├── core-chatstate/             # Optional: InMemoryBotChatStateService
│                               # (replace with Redis/JDBC by providing your own @Bean)
│
├── core-i18n/                  # Optional: i18n support (BotMessageSource, keyboards, Locale injection)
│
├── core/                       # Engine: dispatching, filter chain, argument resolvers,
│                               # return-type handlers, Bot abstract class, autoconfiguration
│
├── longpolling/                # Long-polling transport autoconfiguration
├── webhook/                    # Webhook transport autoconfiguration (Spring MVC)
│
├── messaging-api/              # Kafka + RabbitMQ publisher/consumer transports (optional deps)
│
├── spring-boot-starter/        # One-stop dependency: pulls transports + core + core-i18n
└── samples/                    # Runnable example applications for each transport
```

### Dependency graph

```mermaid
graph TD
    CA[core-api] --> CC[core-chatstate]
    CA --> CI[core-i18n]
    CA --> CO[core-observability]
    CA --> C[core]
    CC --> C
    C --> LP[longpolling]
    C --> WH[webhook]
    C --> MA[messaging-api]
    MA --> SBS[spring-boot-starter]
    LP --> SBS
    WH --> SBS
    CI --> SBS
```

---

## 📋 Requirements

| Requirement | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.x |
| Build tool | Maven or Gradle |

---

## 📦 Installation

Add the starter to your `pom.xml` — it includes all transports and the core engine:

### Maven

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("uz.osoncode.easygram:spring-boot-starter:0.0.5")
```

> For broker publisher/consumer dependencies, see the relevant [module READMEs](#-module-documentation).

---

## 🔀 Transport Selection

Set `easygram.transport` (or env var `TELEGRAM_BOT_TRANSPORT`) to choose the update source:

| Value | Default | Description | Module README |
|---|---|---|---|
| `LONG_POLLING` | ✅ | Polls the Telegram `getUpdates` API | [longpolling/README.md](longpolling/README.md) |
| `WEBHOOK` | | Receives POSTs from Telegram at a configured URL | [webhook/README.md](webhook/README.md) |
| `KAFKA_CONSUMER` | | Consumes JSON updates from a Kafka topic | [messaging-api/README.md](messaging-api/README.md) |
| `RABBIT_CONSUMER` | | Consumes JSON updates from a RabbitMQ queue | [messaging-api/README.md](messaging-api/README.md) |

Only **one** transport is active at a time. See the linked module README for full configuration properties.

---

## 🚀 Quick Start

### 1. Add the dependency and configure

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
```

```yaml
easygram:
  token: ${BOT_TOKEN}
  # transport: LONG_POLLING   # optional; LONG_POLLING is the default
```

### 2. Create your main class

```java
@SpringBootApplication
public class MyBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBotApplication.class, args);
    }
}
```

### 3. Create a handler

```java
@BotController
public class MyBotHandler {

    @BotCommand("/start")
    public String onStart(User user) {
        return "👋 Hello, " + user.getFirstName() + "!";
    }

    @BotDefaultHandler
    public String onDefault() {
        return "I didn't understand that.";
    }
}
```

> For the full handler reference — annotations, parameter injection, return types, filters, and exception handling — see [core/README.md](core/README.md).

---

## ⌨️ Declarative Keyboards & Markups

Decouple keyboard creation from handler logic using the markup registry.

1. **Define markups** in a `@BotConfiguration` class:
   ```java
   @BotConfiguration
   public class MyMarkups {
   
       @BotMarkup("main_menu")
       public ReplyKeyboard mainMenu() {
           return ReplyKeyboardMarkup.builder()
                   .keyboardRow(new KeyboardRow("My Profile"))
                   .build();
       }
   }
   ```

2. **Attach to handlers** with `@BotReplyMarkup`:
   ```java
   @BotCommand("/start")
   @BotReplyMarkup("main_menu")
   public String onStart() {
       return "Welcome!";
   }
   ```

3. **Or attach programmatically** using `PlainReply` / `LocalizedReply`:
   ```java
   @BotCommand("/start")
   public PlainReply onStart() {
       return PlainReply.of("Welcome!").withMarkup("main_menu");
   }
   ```

4. **Remove keyboard** with `@BotClearMarkup`:
   ```java
   @BotCommand("/cancel")
   @BotClearMarkup
   public String onCancel() {
       return "Cancelled.";
   }
   ```
   Or programmatically:
   ```java
   return PlainReply.of("Cancelled.").removeMarkup();
   ```

---

## 🌍 Internationalisation (i18n)

Add `core-i18n` (already included transitively by `spring-boot-starter`) to get:
- `BotMessageSource` (locale-aware messages)
- `BotKeyboardFactory` (localised keyboards)
- `Locale` parameter injection in handlers
- `@BotReplyButton` message-key matching across languages

Minimal configuration:

```yaml
spring:
  messages:
    basename: messages/bot
    encoding: UTF-8

easygram:
  i18n:
    default-locale: en
```

See [core-i18n/README.md](core-i18n/README.md) for full usage.

---

## 📚 Module Documentation

| Module | Artifact ID | Documentation |
|---|---|---|
| Core API (contracts) | `core-api` | [core-api/README.md](core-api/README.md) |
| Core Engine | `core` | [core/README.md](core/README.md) |
| Long-Polling Transport | `longpolling` | [longpolling/README.md](longpolling/README.md) |
| Webhook Transport | `webhook` | [webhook/README.md](webhook/README.md) |
| Chat State | `core-chatstate` | [core-chatstate/README.md](core-chatstate/README.md) |
| Internationalisation | `core-i18n` | [core-i18n/README.md](core-i18n/README.md) |
| Observability | `core-observability` | [core-observability/README.md](core-observability/README.md) |
| Messaging (Kafka + RabbitMQ) | `messaging-api` | [messaging-api/README.md](messaging-api/README.md) |
| Spring Boot Starter | `spring-boot-starter` | [spring-boot-starter/README.md](spring-boot-starter/README.md) |
| Samples (runnable apps) | — | [samples/README.md](samples/README.md) |

---

## 🧵 Threading Model

Understanding how Easygram processes updates prevents common performance issues.

### How updates flow through the executor

Every incoming update (from any transport) is submitted to an `ExecutorService`:

```
Telegram  ──▶  transport thread  ──▶  ExecutorService.submit()  ──▶  worker thread
                                                                         │
                                                                         ▼
                                                              BotFilter chain
                                                              BotDispatcher
                                                              @BotController method
                                                              TelegramClient.execute()
```

The **default pool size is `max(2, availableProcessors())`** — automatically sized for the host. Telegram delivers up to 100 updates per long-poll response; a single-threaded executor would process them serially. The fixed pool processes them in parallel, up to the pool size.

### Blocking inside handlers

The worker thread is held for the entire filter chain + handler invocation. Any blocking operation (database query, HTTP call, file I/O) inside a handler method holds that thread. If all worker threads are blocked simultaneously, the queue grows unbounded.

**Guidance:**
- Keep handlers non-blocking where possible — `String` replies are serialised on the same thread
- For heavy work, delegate to a separate `@Async` bean or submit to a dedicated executor
- Override the default executor to tune the pool size:

```java
@Bean
EasygramExecutorServiceProvider executorServiceProvider() {
    ExecutorService pool = Executors.newFixedThreadPool(16);
    return () -> pool;
}
```

On JDK 21+ you can use virtual threads:

```java
@Bean
EasygramExecutorServiceProvider executorServiceProvider() {
    return () -> Executors.newVirtualThreadPerTaskExecutor();
}
```

### Thread safety of handlers

Handler beans are Spring singletons. If your handler class has mutable fields, access to them must be thread-safe (use `AtomicReference`, `ConcurrentHashMap`, or synchronisation). The framework itself is stateless between updates — `BotRequest`, `BotResponse`, and resolved arguments are created fresh per invocation.

---

## 🏗️ Building from Source

```bash
git clone https://github.com/oson-code/easygram.git
cd easygram

# Install all library modules to local Maven repository
mvn clean install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

# Build runnable sample apps (optional)
cd samples
mvn package -DskipTests
```

See [samples/README.md](samples/README.md) for what each sample does and the config to run it.

---

## 🤝 Contributing

Contributions are welcome! Please [open an issue](https://github.com/oson-code/easygram/issues) or submit a merge request. Follow the existing code style, add tests for new behaviour, and describe your changes clearly.

---

## 📄 License

MIT License — Copyright © 2026 [OSONCODE](https://osoncode.uz)
