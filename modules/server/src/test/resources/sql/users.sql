CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    nickname        TEXT NOT NULL,
    email           TEXT NOT NULL,
    hashed_password TEXT NOT NULL,
    first_name      TEXT NULL,
    last_name       TEXT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NULL
);