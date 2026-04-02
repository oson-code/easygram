// @ts-check
/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Easygram',
  tagline: 'Build Telegram bots. No boilerplate.',
  url: 'https://easygram.osoncode.uz',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },
  favicon: 'img/favicon.ico',
  organizationName: 'oson-code',
  projectName: 'easygram',

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
              label: '0.0.4',
              path: '/',
            },
            '0.0.3': {
              label: '0.0.3',
              path: '/0.0.3',
            },
            '0.0.2': {
              label: '0.0.2',
              path: '/0.0.2',
            },
            '0.0.1': {
              label: '0.0.1',
              path: '/0.0.1',
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
      image: 'img/docusaurus-social-card.jpg',
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
                label: 'Architecture',
                to: '/docs/architecture',
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

