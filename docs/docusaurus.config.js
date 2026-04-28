// @ts-check
/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Easygram',
  tagline: 'Build Telegram bots with Spring Boot — no boilerplate.',
  url: 'https://easygram.osoncode.uz',
  baseUrl: '/',
  trailingSlash: false,
  onBrokenLinks: 'throw',
  favicon: 'img/favicon.ico',
  organizationName: 'oson-code',
  projectName: 'easygram',

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  headTags: [
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        type: 'image/svg+xml',
        href: '/img/favicon.svg',
      },
    },
  ],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/oson-code/easygram/tree/main/docs',
          lastVersion: 'current',
          versions: {
            current: {
              label: '0.0.6',
              path: '/',
            },
          },
        },
        blog: {
          showReadingTime: true,
          editUrl: 'https://github.com/oson-code/easygram/tree/main/docs',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
        sitemap: {
          changefreq: 'weekly',
          priority: 0.5,
          ignorePatterns: ['/tags/**', '/0.0.1/**', '/0.0.2/**', '/0.0.3/**', '/0.0.4/**', '/0.0.5/**'],
          filename: 'sitemap.xml',
        },
      }),
    ],
  ],
  plugins: [
    [
      '@docusaurus/plugin-client-redirects',
      {
        redirects: [],
      },
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      metadata: [
        { name: 'keywords', content: 'telegram bot, spring boot, java, framework, annotation-driven, webhook, long-polling, kafka, rabbitmq' },
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:site', content: '@osoncode' },
        { name: 'twitter:image', content: 'https://easygram.osoncode.uz/img/og-image.png' },
        { property: 'og:type', content: 'website' },
        { property: 'og:image', content: 'https://easygram.osoncode.uz/img/og-image.png' },
        { property: 'og:image:width', content: '1200' },
        { property: 'og:image:height', content: '630' },
        { property: 'og:site_name', content: 'Easygram' },
      ],
      image: 'img/og-image.png',
      navbar: {
        title: 'Easygram',
        style: 'dark',
        logo: {
          alt: 'Easygram Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docsVersionDropdown',
            position: 'right',
          },
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Docs',
          },
          { to: '/blog', label: 'Blog', position: 'left' },
          {
            href: 'https://github.com/oson-code/easygram',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Getting Started',
                to: '/docs/intro',
              },
              {
                label: 'Quick Start',
                to: '/docs/quick-start',
              },
              {
                label: 'Architecture',
                to: '/docs/architecture',
              },
              {
                label: 'API Reference',
                to: '/docs/api-reference',
              },
            ],
          },
          {
            title: 'Guides',
            items: [
              {
                label: 'Handler Annotations',
                to: '/docs/core-concepts/handlers',
              },
              {
                label: 'Return Types',
                to: '/docs/core-concepts/return-types',
              },
              {
                label: 'Chat State',
                to: '/docs/core-concepts/chat-state',
              },
              {
                label: 'Transports',
                to: '/docs/transports/long-polling-guide',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitHub Issues',
                href: 'https://github.com/oson-code/easygram/issues',
              },
              {
                label: 'GitHub Discussions',
                href: 'https://github.com/oson-code/easygram/discussions',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Blog',
                to: '/blog',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/oson-code/easygram',
              },
              {
                label: 'Maven Central',
                href: 'https://central.sonatype.com/artifact/uz.osoncode.easygram/spring-boot-starter',
              },
            ],
          },
        ],
        copyright: `Copyright ${new Date().getFullYear()} OSONCODE. MIT License.`,
      },
      prism: {
        additionalLanguages: ['java', 'yaml', 'markup', 'bash', 'sql', 'properties'],
      },
    }),
};

module.exports = config;

