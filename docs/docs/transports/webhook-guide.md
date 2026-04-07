---
id: webhook-guide
title: Webhook Transport
---

# Webhook Transport Guide

Webhook transport lets Telegram **push** updates to your bot via HTTPS POST requests, eliminating polling latency. Ideal for production deployments with public HTTPS access.

## How It Works

1. On startup, Easygram calls Telegram's `setWebhook` API with your configured URL
2. When a user sends a message, Telegram makes an HTTPS POST to your webhook endpoint
3. Your bot processes the update synchronously and returns HTTP 200
4. No polling — Telegram delivers updates immediately

## Dependencies

Webhook requires `spring-web` (included in `spring-boot-starter-web`) in addition to the Easygram starter:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## Configuration

**application.yml:**
```yaml
easygram:
  token: ${BOT_TOKEN}
  update:
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook
      path: /webhook
      secret-token: ${WEBHOOK_SECRET}
      max-connections: 40
      drop-pending-updates: false
      unregister-on-shutdown: false
```

**application.properties:**
```properties
easygram.token=${BOT_TOKEN}
easygram.update.transport=WEBHOOK
easygram.update.webhook.url=https://your-domain.com/webhook
easygram.update.webhook.path=/webhook
easygram.update.webhook.secret-token=${WEBHOOK_SECRET}
```

## Configuration Options

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.update.transport` | **Yes** | — | Must be `WEBHOOK` |
| `easygram.update.webhook.url` | **Yes** | — | Public HTTPS URL Telegram delivers updates to |
| `easygram.update.webhook.path` | No | `/webhook` | Local HTTP endpoint path that receives the POST |
| `easygram.update.webhook.secret-token` | No | — | Validates the `X-Telegram-Bot-Api-Secret-Token` header |
| `easygram.update.webhook.max-connections` | No | — | Max simultaneous Telegram connections (1–100) |
| `easygram.update.webhook.drop-pending-updates` | No | `false` | Discard queued updates on webhook registration |
| `easygram.update.webhook.unregister-on-shutdown` | No | `false` | Call `deleteWebhook` on application shutdown |

## HTTPS Setup

### Option 1: Reverse Proxy with Let's Encrypt (Recommended)

Use Nginx to terminate TLS — your Spring Boot app stays on plain HTTP internally.

```bash
# Install certbot and get a certificate
certbot certonly --standalone -d your-domain.com
```

**nginx.conf:**
```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location /webhook {
        proxy_pass http://bot:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Option 2: Spring Boot with Embedded TLS

For environments without a reverse proxy:

```yaml
server:
  ssl:
    key-store: /path/to/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12

easygram:
  token: ${BOT_TOKEN}
  update:
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook
      secret-token: ${WEBHOOK_SECRET}
```

## Example: Webhook Bot

```java
@SpringBootApplication
public class WebhookBotApp {
    public static void main(String[] args) {
        SpringApplication.run(WebhookBotApp.class, args);
    }
}

@BotController
public class WebhookBot {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello via webhook, " + user.getFirstName() + "!";
    }

    @BotDefaultHandler
    public String fallback() {
        return "Send /start to begin.";
    }
}
```

**application.yml:**
```yaml
server:
  port: 8080

easygram:
  token: ${BOT_TOKEN}
  update:
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook
```

## Testing Webhook Locally

Use a tunneling tool to expose your local server over HTTPS during development.

### Option 1: jprq

[jprq](https://jprq.io) is a lightweight tunneling tool.

```bash
# Install
curl -fsSL https://jprq.io/install.sh | sudo bash

# Authenticate (get your token at https://jprq.io/auth)
jprq auth <your-auth-token>

# Start tunnel
jprq http 8080
# Outputs: https://<subdomain>.jprq.app
```

Run your bot using the tunnel URL as Spring property names:

```bash
easygram.update.transport=WEBHOOK \
easygram.update.webhook.url=https://<subdomain>.jprq.app/webhook \
mvn spring-boot:run
```

### Option 2: ngrok

```bash
brew install ngrok
ngrok http 8080
# Outputs: https://abc123.ngrok.io
```

```bash
easygram.update.transport=WEBHOOK \
easygram.update.webhook.url=https://abc123.ngrok.io/webhook \
mvn spring-boot:run
```

## Docker Deployment

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q install -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S bot && adduser -S bot -G bot
USER bot
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**docker-compose.yml (with Nginx):**
```yaml
services:
  bot:
    build: .
    environment:
      easygram.token: ${BOT_TOKEN}
      easygram.update.transport: WEBHOOK
      easygram.update.webhook.url: https://your-domain.com/webhook
      easygram.update.webhook.secret-token: ${WEBHOOK_SECRET}
    networks:
      - botnet
    restart: unless-stopped

  nginx:
    image: nginx:latest
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on:
      - bot
    networks:
      - botnet
    restart: unless-stopped

networks:
  botnet:
```

## Advantages

- ✅ **Low latency** — Telegram pushes updates immediately on user action
- ✅ **Efficient** — No polling; Telegram only calls your server when needed
- ✅ **Scalable** — Standard HTTP POST; works with any load balancer
- ✅ **Production-grade** — Industry standard for Telegram bots

## Disadvantages

- ⚠️ Requires a valid HTTPS certificate (Let's Encrypt is free)
- ⚠️ Requires a public domain or IP address accessible from the internet
- ⚠️ More complex initial setup vs long-polling

## Production Checklist

- [ ] Valid SSL certificate (Let's Encrypt recommended)
- [ ] DNS record pointing domain to your server IP
- [ ] Firewall allows inbound port 443
- [ ] Bot token and webhook secret in environment variables (not committed to source)
- [ ] `easygram.update.webhook.unregister-on-shutdown: true` during zero-downtime deploys
- [ ] Monitoring and alerting configured (Spring Actuator + your metrics platform)
- [ ] Log aggregation with MDC fields: `bot.update.id`, `bot.chat.id`, `bot.user.id`
- [ ] Rate limiting / DDoS protection at the reverse proxy level

---

Next: Learn about [Kafka Consumer Transport](kafka-consumer-guide) for event-driven architectures.

