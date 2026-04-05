-- Baseline schema for Penny Pilot v1.0
-- Matches the Hibernate-generated schema as of the MVP release.
--
-- Migration convention:
--   Production migrations live here: db/migration/V{N}__{description}.sql
--   Never modify an existing migration file — always create a new one.
--
-- Note: SQLite requires INTEGER (not BIGINT) for PRIMARY KEY AUTOINCREMENT.
-- SQLite INTEGER is always 64-bit signed, matching Java Long.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS providers (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR NOT NULL UNIQUE,
    description VARCHAR
);

CREATE TABLE IF NOT EXISTS categories (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    name    VARCHAR NOT NULL,
    icon    VARCHAR,
    color   VARCHAR
);

CREATE TABLE IF NOT EXISTS category_rules (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    match_pattern VARCHAR NOT NULL,
    category_id   BIGINT NOT NULL,
    priority      INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             BIGINT NOT NULL,
    provider_id         BIGINT NOT NULL,
    provider_account_id VARCHAR NOT NULL,
    account_name        VARCHAR NOT NULL,
    balance_cents       INTEGER,
    last_synced_at      TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          BIGINT NOT NULL,
    account_id       BIGINT NOT NULL,
    category_id      BIGINT,
    amount_cents     INTEGER NOT NULL,
    transaction_type VARCHAR NOT NULL,
    description      VARCHAR NOT NULL,
    merchant_name    VARCHAR,
    date             VARCHAR NOT NULL,
    external_id      VARCHAR
);

CREATE TABLE IF NOT EXISTS user_provider_credentials (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    credential  VARCHAR NOT NULL
);

-- Seed the SimpleFIN provider (production provider).
-- MOCK provider is seeded at application startup only when the dev profile is active.
INSERT OR IGNORE INTO providers (name, description) VALUES ('SIMPLEFIN', 'SimpleFIN Bridge');
