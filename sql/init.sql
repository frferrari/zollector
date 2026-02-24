-- CREATE DATABASE zollector_dev;
\c zollector_dev;

-- CREATE EXTENSION "pgcrypto";

CREATE TABLE referential.categories
(
    id         bigserial PRIMARY KEY                  NOT NULL,
    is_active  boolean                                NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE referential.category_translations
(
    id          bigserial PRIMARY KEY NOT NULL,
    category_id bigint references referential.categories (id) on delete cascade,
    language    text                  NOT NULL,
    name        text                  NOT NULL,
    description text,
    slug        text                  NOT NULL
);

CREATE TABLE collections (
    id uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL,
    category_id BIGINT NOT NULL,
    family_id   BIGINT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    year_start  INT NULL,
    year_end    INT NULL,
    slug        TEXT NOT NULL,
    image       TEXT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NULL
);

CREATE TABLE users (
    id uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname        TEXT NOT NULL,
    email           TEXT NOT NULL,
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
