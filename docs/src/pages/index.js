import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import styles from './index.module.css';
import HomepageFeatures from '../components/HomepageFeatures';

function HomepageHeader() {
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <h1 className="hero__title">Easygram</h1>
        <p className="hero__subtitle">Build Telegram bots. No boilerplate.</p>
        <p className={styles.heroDescription}>
          A Spring Boot framework for building Telegram bots with annotation-driven routing,
          pluggable transports, and zero configuration boilerplate.
        </p>
        <div className={clsx(styles.buttons, styles.heroButtons)}>
          <Link className="button button--secondary button--lg" to="/docs/intro">
            Get Started
          </Link>
          <Link className="button button--outline button--lg" to="https://github.com/oson-code/easygram">
            View on GitHub
          </Link>
        </div>
      </div>
    </header>
  );
}

const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'SoftwareApplication',
  name: 'Easygram',
  url: 'https://easygram.osoncode.uz',
  downloadUrl: 'https://central.sonatype.com/artifact/uz.osoncode.easygram/spring-boot-starter',
  applicationCategory: 'DeveloperApplication',
  operatingSystem: 'JVM',
  programmingLanguage: 'Java',
  runtimePlatform: 'Spring Boot 3',
  description:
    'Easygram is a Spring Boot framework for building Telegram bots with annotation-driven routing, pluggable transports (long-polling, webhook, Kafka, RabbitMQ), chat state management, i18n, and Micrometer observability.',
  keywords:
    'telegram bot, spring boot, java, annotation-driven, webhook, long-polling, kafka, rabbitmq, chatbot framework',
  softwareVersion: '0.0.5',
  license: 'https://opensource.org/licenses/MIT',
  author: {
    '@type': 'Organization',
    name: 'OSONCODE',
    url: 'https://osoncode.uz',
  },
};

export default function Home() {
  return (
    <Layout
      title="Spring Boot Telegram Bot Framework"
      description="Easygram is a Spring Boot framework for building Telegram bots with annotation-driven routing, pluggable transports, chat state management, and zero boilerplate."
    >
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <HomepageHeader />
      <main>
        <HomepageFeatures />

        <section className={styles.whySection}>
          <div className="container">
            <div className="row">
              <div className="col col--6">
                <h2>Why Easygram?</h2>
                <ul className={styles.whyList}>
                  <li><strong>Minimal Setup</strong> — One dependency, one property, and you're running</li>
                  <li><strong>Annotation-Driven</strong> — Declarative routing with <code>@BotCommand</code>, <code>@BotText</code>, <code>@BotTextPattern</code>, and more</li>
                  <li><strong>Type-Safe Injection</strong> — Parameters resolved at startup with compile-time checking</li>
                  <li><strong>Stateful Flows</strong> — Multi-step wizards with built-in chat state management</li>
                  <li><strong>Pluggable</strong> — Swap transports, backends, and services without changing handlers</li>
                  <li><strong>Observable</strong> — Micrometer metrics, distributed tracing, and health indicators built in</li>
                </ul>
              </div>
              <div className="col col--6">
                <div className={styles.codePreview}>
                  <pre>{`@BotController
public class MyBot {

  @BotCommand("/start")
  public String onStart(User user) {
    return "Hello, " + user.getFirstName();
  }

  @BotTextPattern("\\\\d+")
  @BotChatState("WAITING_AGE")
  @BotForwardChatState("DONE")
  public String onAge(@BotTextValue String age) {
    return "Got it. You are " + age + " years old.";
  }

  @BotDefaultHandler
  public String fallback() {
    return "I did not understand that.";
  }
}`}</pre>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.quickLinksSection}>
          <div className="container">
            <h2 className={styles.sectionTitle}>Quick Links</h2>
            <div className="row">
              {[
                { title: 'Documentation', desc: 'Complete guides for every feature and transport', link: '/docs/intro', label: 'Read the docs' },
                { title: 'Quick Start', desc: 'Build your first bot in five minutes', link: '/docs/quick-start', label: 'Get started' },
                { title: 'Examples', desc: 'Runnable reference implementations for each use case', link: '/docs/examples/echo-bot', label: 'View examples' },
                { title: 'GitHub', desc: 'Source code, issue tracker, and release notes', link: 'https://github.com/oson-code/easygram', label: 'Open GitHub' },
              ].map(({ title, desc, link, label }) => (
                <div key={title} className="col col--3">
                  <div className={styles.quickCard}>
                    <h3 className={styles.quickCardTitle}>{title}</h3>
                    <p className={styles.quickCardDesc}>{desc}</p>
                    <Link to={link}>{label}</Link>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
