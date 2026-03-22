---
id: webhook-guide
title: Webhook Transport
---

# Webhook Transport Guide

Webhook transport lets Telegram **push** updates to your bot via HTTPS, eliminating polling latency.

## How It Works

1. You provide Telegram with your bot's webhook URL (must be HTTPS)
2. When user sends a message, Telegram makes an HTTP POST to your webhook
3. Your bot receives and processes the update
4. You respond with Telegram API calls

## Configuration

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook/telegram
      port: 8080
      allowed-updates:
        - message
        - callback_query
        - inline_query
```

## Configuration Options

| Property | Required | Description |
|---|---|---|
| `url` | Yes | Your webhook URL (must be HTTPS) |
| `port` | No | Port to listen on (default: 8080) |
| `path` | No | Path for webhook endpoint (default: /telegram) |
| `allowed-updates` | No | Types of updates to receive |

## HTTPS Setup

### 1. Obtain SSL Certificate

**Option A: Let's Encrypt (Free)**
```bash
certbot certonly --standalone -d your-domain.com
```

**Option B: Self-signed (Testing Only)**
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout privkey.pem -out fullchain.pem
```

### 2. Configure Spring Boot

**application.yml:**
```yaml
server:
  ssl:
    key-store: /path/to/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat

telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook
      port: 8443
```

### 3. Deploy Behind Reverse Proxy (Recommended)

Use Nginx to terminate TLS:

**nginx.conf:**
```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    
    location /webhook {
        proxy_pass http://localhost:8080;
    }
}
```

## Docker Deployment

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jdk
COPY target/bot.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml with Nginx:**
```yaml
version: '3.8'

services:
  bot:
    build: .
    environment:
      BOT_TOKEN: ${BOT_TOKEN}
      TELEGRAM_BOT_TRANSPORT: WEBHOOK
      TELEGRAM_BOT_WEBHOOK_URL: https://your-domain.com/webhook
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
}
```

**application.yml:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /

telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://your-domain.com/webhook
      allowed-updates:
        - message
        - callback_query
```

## Testing Webhook Locally

Use ngrok to expose local server:

```bash
# Install ngrok
brew install ngrok

# Start tunnel
ngrok http 8080
# Outputs: https://abc123.ngrok.io

# Update config
TELEGRAM_BOT_WEBHOOK_URL=https://abc123.ngrok.io/webhook \
TELEGRAM_BOT_TRANSPORT=WEBHOOK \
mvn spring-boot:run
```

## Advantages

✅ **Low latency** — Telegram pushes updates immediately
✅ **Efficient** — No constant polling
✅ **Scalable** — Handles high traffic
✅ **Production-ready** — Industry standard

## Disadvantages

❌ Requires HTTPS certificate
❌ More complex setup
❌ Requires public domain
❌ Need to handle TLS/certificate renewal

## Production Checklist

- [ ] Valid SSL certificate (Let's Encrypt recommended)
- [ ] Domain DNS pointing to your server
- [ ] Firewall allows port 443 inbound
- [ ] Bot token secured in environment variables
- [ ] Monitoring/alerting set up
- [ ] Log aggregation configured
- [ ] Rate limiting/DDoS protection
- [ ] Regular security updates

---

Next: Learn about [Kafka Consumer Transport](kafka-consumer-guide).
