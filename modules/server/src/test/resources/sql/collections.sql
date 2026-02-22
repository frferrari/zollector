CREATE TABLE collections (
    id uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL,
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    year_start  INT NULL,
    year_end    INT NULL,
    slug        TEXT NOT NULL,
    image       TEXT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NULL
);
