import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Zero Boilerplate',
    description: (
      <>
        One dependency, one property. Annotation-driven routing eliminates manual update
        polling loops and dispatcher wiring. See the{' '}
        <Link to="/docs/quick-start">Quick Start guide</Link>.
      </>
    ),
  },
  {
    title: 'Pluggable Transports',
    description: (
      <>
        Switch between long-polling, webhook, Kafka consumer, or RabbitMQ consumer with a
        single configuration property — no code changes required. See{' '}
        <Link to="/docs/transports/long-polling-guide">Transports</Link>.
      </>
    ),
  },
  {
    title: 'Declarative Routing',
    description: (
      <>
        Route by command, text, regex pattern, callback query, contact, or location.
        Parameters such as <code>User</code>, <code>Chat</code>, and custom domain objects
        are resolved and injected automatically. See{' '}
        <Link to="/docs/core-concepts/handlers">Handlers</Link>.
      </>
    ),
  },
  {
    title: 'Stateful Conversations',
    description: (
      <>
        Build multi-step registration flows and wizards with <code>@BotChatState</code> and{' '}
        <code>@BotForwardChatState</code>. Backed by an in-memory store by default; replace
        with Redis or a database without modifying handlers. See{' '}
        <Link to="/docs/core-concepts/chat-state">Chat State</Link>.
      </>
    ),
  },
  {
    title: 'Fully Extensible',
    description: (
      <>
        Every bean uses <code>@ConditionalOnMissingBean</code>. Override filters, argument
        resolvers, return-type handlers, state services, and more by registering your own{' '}
        <code>@Bean</code>. See <Link to="/docs/advanced/custom-filters">Custom Filters</Link>.
      </>
    ),
  },
  {
    title: 'Production Ready',
    description: (
      <>
        Internationalization, <code>@BotControllerAdvice</code> exception handling, a
        prioritized filter pipeline, Micrometer observability, and broker publishing to
        Kafka or RabbitMQ. See{' '}
        <Link to="/docs/advanced/observability">Observability</Link>.
      </>
    ),
  },
];

function Feature({title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="padding-horiz--md feature-card">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
