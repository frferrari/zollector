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