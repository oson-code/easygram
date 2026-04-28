module.exports = {
  tutorialSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'intro',
        'architecture',
        'quick-start',
      ],
    },
    {
      type: 'category',
      label: 'Core Concepts',
      items: [
        'core-concepts/handlers',
        'core-concepts/filters',
        'core-concepts/parameter-injection',
        'core-concepts/chat-state',
        'core-concepts/return-types',
        'core-concepts/exception-handling',
      ],
    },
    {
      type: 'category',
      label: 'Transports',
      items: [
        'transports/long-polling-guide',
        'transports/webhook-guide',
        'transports/kafka-consumer-guide',
        'transports/rabbitmq-consumer-guide',
      ],
    },
    {
      type: 'category',
      label: 'Advanced',
      items: [
        'advanced/markup-system',
        'advanced/custom-filters',
        'advanced/custom-argument-resolvers',
        'advanced/custom-return-handlers',
        {
          type: 'doc',
          id: 'advanced/bot-reply-action',
          customProps: { badge: 'new' },
        },
        'advanced/validation',
        'advanced/i18n-setup',
        'advanced/chat-state-backends',
        'advanced/broker-publishing',
        'advanced/observability',
        'advanced/dynamic-callback-query',
      ],
    },
    {
      type: 'category',
      label: 'Examples',
      items: [
        'examples/echo-bot',
        'examples/registration-wizard',
        'examples/i18n-registration-bot',
        'examples/kafka-producer-bot',
        'examples/producer-bot',
        'examples/webhook-bot',
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      items: [
        'api-reference',
        'faq',
        'contributing',
      ],
    },
    {
      type: 'category',
      label: 'Migration',
      items: [
        {
          type: 'doc',
          id: 'whatsnew-0.0.6',
          label: "What's New in 0.0.6",
          customProps: { badge: 'new' },
        },
        'migration/0.0.1-to-0.0.2',
        'migration/0.0.2-to-0.0.3',
        'migration/0.0.3-to-0.0.4',
        'migration/0.0.4-to-0.0.5',
        'migration/0.0.5-to-0.0.6',
      ],
    },
  ],
};
