CREATE SCHEMA referential;

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
