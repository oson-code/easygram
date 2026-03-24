# Easygram Documentation

Comprehensive documentation for [Easygram](https://github.com/oson-code/easygram) — the
annotation-driven Spring Boot framework for building Telegram bots. Built with
[Docusaurus 2](https://docusaurus.io/).

---

## Quick Start (Docker — no local Node.js needed)

Everything runs inside Docker. You do not need Node.js, npm, or yarn installed on your machine.

### Development server (live reload)

```bash
cd docs/
docker compose up
```

Open [http://localhost:3000](http://localhost:3000).

Changes to any file under `docs/` are reflected immediately thanks to the volume mount and
Docusaurus's hot-reload. The `node_modules` directory is kept inside the container via an
anonymous volume — it is never written to your local disk.

### Production build

```bash
cd docs/
docker compose --profile production up docusaurus-prod
```

The static site is written to `./build/`. Serve it with any static file host.

### Stop the server

```bash
docker compose down
```

---

## Docker Compose Reference

The `docker-compose.yml` in this directory defines two services:

| Service | Profile | Purpose |
|---|---|---|
| `docusaurus` | *(default)* | Development server with hot-reload on port 3000 |
| `docusaurus-prod` | `production` | Production build — outputs to `./build/` |

Both services use the official `node:20-alpine` image. The `node_modules` directory lives
inside the container only (anonymous volume), so your local working tree stays clean.

---

## Directory Structure

```
docs/
 docs/ # Documentation source (Markdown)
    intro.md # What is Easygram?
    architecture.md # System design and module structure
    quick-start.md # Build your first bot in 5 minutes
    api-reference.md # All annotations, interfaces, models
    faq.md # Frequently asked questions
   
    core-concepts/ # 6 core-concept guides
       handlers.md # All handler annotations and routing tiers
       filters.md # Filter pipeline and middleware
       parameter-injection.md # Automatic parameter resolution
       chat-state.md # Stateful workflows
       return-types.md # All supported return types
       exception-handling.md # @BotExceptionHandler
   
    transports/ # 4 transport guides
       long-polling-guide.md # Default polling transport
       webhook-guide.md # HTTPS webhook transport
       kafka-consumer-guide.md # Kafka consumer transport
       rabbitmq-consumer-guide.md # RabbitMQ consumer transport
   
    advanced/ # 7 advanced topics
       custom-filters.md # BotFilter — auth, rate-limiting, logging
       custom-argument-resolvers.md # BotArgumentResolver extension
       custom-return-handlers.md # BotReturnTypeHandler extension
       i18n-setup.md # Internationalization with core-i18n
       chat-state-backends.md # Redis, JDBC, custom state backends
       broker-publishing.md # Kafka/RabbitMQ update forwarding
       observability.md # Micrometer, Prometheus, tracing
   
    examples/ # 4 runnable examples
        echo-bot.md # Minimal echo bot (long-polling)
        registration-wizard.md # Multi-step state machine
        kafka-producer-bot.md # Updates forwarded to Kafka
        webhook-bot.md # HTTPS webhook transport

 blog/ # Release notes and announcements
 src/
    components/ # React components (HomepageFeatures)
    css/ # Custom CSS (branding, colors)
    pages/ # Custom pages (landing page index.js)
 static/ # Static assets (images, favicons)
 docusaurus.config.js # Site configuration (title, navbar, footer)
 sidebars.js # Navigation sidebar structure
 package.json # Node dependencies and scripts
 docker-compose.yml # Docker setup (dev + prod)
 README.md # This file
```

---

## Writing Documentation

### Front matter

Every doc must start with YAML front matter:

```markdown
---
id: my-page
title: My Page Title
---

# My Page Title

Content here...
```

### Code blocks

Use fenced code blocks with a language identifier for syntax highlighting:

````markdown
```java
@BotCommand("/start")
public String onStart() {
    return "Hello!";
}
```
````

Supported languages: `java`, `yaml`, `xml`, `json`, `bash`, `sql`, `properties`, `javascript`,
`typescript`, and more.

### Internal links

```markdown
[Quick Start](quick-start) # same directory
[Architecture](../architecture) # parent directory
[Parameter Injection](../core-concepts/parameter-injection) # subdirectory
```

Docusaurus resolves `.md` extensions automatically — omit them in links.

### Callout blocks (admonitions)

```markdown
:::tip
This is a tip.
:::

:::note
This is a note.
:::

:::warning
This is a warning.
:::

:::danger
This is a danger alert.
:::
```

### Tables

```markdown
| Column 1 | Column 2 | Column 3 |
|---|---|---|
| Cell 1 | Cell 2 | Cell 3 |
```

---

## Configuration Files

### docusaurus.config.js

Main site configuration — title, tagline, base URL, navbar, footer, and code highlighting.
Edit here to change branding or add new navbar items.

### sidebars.js

Navigation structure. Add new pages by inserting the doc `id` into the appropriate `items`
array. The sidebar is divided into six categories:

1. **Getting Started** — intro, architecture, quick-start
2. **Core Concepts** — handlers, filters, parameter-injection, chat-state, return-types, exception-handling
3. **Transports** — long-polling-guide, webhook-guide, kafka-consumer-guide, rabbitmq-consumer-guide
4. **Advanced** — custom-filters, custom-argument-resolvers, custom-return-handlers, i18n-setup, chat-state-backends, broker-publishing, observability
5. **Examples** — echo-bot, registration-wizard, kafka-producer-bot, webhook-bot
6. **Reference** — api-reference, faq

---

## Versioning

When releasing a new framework version:

```bash
# Inside the container (or with local Node.js):
npx docusaurus docs:version 0.0.2
```

This creates:
- `versioned_docs/version-0.0.2/`
- `versioned_sidebars/version-0.0.2-sidebars.json`
- Updates `versions.json`

Users can then switch versions via the dropdown in the navbar.

---

## Customization

### Colors

Edit `src/css/custom.css`:

```css
:root {
  --ifm-color-primary: #5e7ce0;
  --ifm-color-primary-dark: #4a6cc8;
  /* ... more color variables */
}
```

### Navbar and footer

Edit `docusaurus.config.js`:

```javascript
navbar: {
  title: 'Easygram',
  items: [
    { label: 'Docs', type: 'docSidebar', sidebarId: 'tutorialSidebar' },
    { label: 'GitHub', href: 'https://github.com/oson-code/easygram', position: 'right' }
  ]
},
footer: {
  links: [ ... ],
  copyright: 'Copyright 2026 Easygram Contributors'
}
```

---

## Deployment

### Static hosting (Netlify, Vercel, Cloudflare Pages)

```bash
# Build with Docker
docker compose --profile production up docusaurus-prod

# Upload ./build/ to your hosting provider
```

### GitHub Pages

Update `docusaurus.config.js`:
```javascript
url: 'https://oson-code.github.io',
baseUrl: '/easygram/',
organizationName: 'oson-code',
projectName: 'easygram',
```

Then:
```bash
# With Node.js locally:
npm run deploy

# Or with Docker — build first, then push to gh-pages branch manually
```

---

## Troubleshooting

### Port 3000 already in use

Edit `docker-compose.yml` and change `"3000:3000"` to `"3001:3000"`, then:

```bash
docker compose up
# Open http://localhost:3001
```

### node_modules missing or stale

```bash
docker compose down -v # Remove anonymous volumes (clears node_modules)
docker compose up # Fresh install
```

### Links returning 404

- Check file path and that the `id` in front matter matches the sidebar entry
- Use relative paths: `../` to go up one directory
- Verify `sidebars.js` includes the page `id`

### CSS not updating

```bash
docker compose down && docker compose up
```

### Build fails

```bash
docker compose --profile production up docusaurus-prod 2>&1 | tail -50
```

---

## Contributing

To update documentation:

1. Edit Markdown files in `docs/`
2. Run `docker compose up` to preview changes at [http://localhost:3000](http://localhost:3000)
3. Verify the production build: `docker compose --profile production up docusaurus-prod`
4. Submit a PR with your changes

**Guidelines:**
- Keep sections focused and concise
- Use code examples liberally — real code from the samples directory when possible
- Include links to related docs
- Update `sidebars.js` if adding new pages
- Write for users who are new to the framework

---

## Learn More

- [Docusaurus Documentation](https://docusaurus.io/docs)
- [Markdown Guide](https://www.markdownguide.org/)
- [Easygram GitHub](https://github.com/oson-code/easygram)

## License

MIT — same as Easygram.
