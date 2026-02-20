-- CREATE DATABASE zollector_dev;
\c zollector_dev;

-- CREATE EXTENSION "pgcrypto";

CREATE TABLE collections (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    year_start  INT NULL,
    year_end    INT NULL,
    slug        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NULL
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    nickname        TEXT UNIQUE NOT NULL,
    email           TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL,
    first_name      TEXT NULL,
    last_name       TEXT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NULL
);

CREATE TABLE recovery_tokens (
    email           TEXT PRIMARY KEY NOT NULL,
    token           TEXT NOT NULL,
    expiration      BIGINT NOT NULL
);

-- ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);
-- ALTER TABLE users ADD CONSTRAINT users_nickname_unique UNIQUE (nickname);

