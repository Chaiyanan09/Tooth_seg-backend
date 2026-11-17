-- V1: initial schema (replaces the MongoDB collections "users" and "prediction_history")
-- Flyway runs this file once and records it in the table flyway_schema_history.
-- Never edit this file after it has run on a database: add V2__..., V3__... instead.

CREATE TABLE users (
    id                     uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name              text        NOT NULL,
    email                  text        NOT NULL UNIQUE,
    password_hash          text        NOT NULL,
    reset_token_hash       text,
    reset_token_expires_at timestamptz,
    created_at             timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE prediction_history (
    id                    uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at            timestamptz NOT NULL DEFAULT now(),
    model_name            text        NOT NULL DEFAULT 'HTC_SwinT_FPN',
    path                  text,
    inference_ms          bigint      NOT NULL DEFAULT 0 CHECK (inference_ms >= 0),

    -- compact result
    present_fdi           text[],
    missing_fdi           text[],
    present_top_score_map jsonb,

    -- Cloudinary references (images are stored on Cloudinary, not in the database)
    input_public_id       text,
    input_version         bigint,
    input_format          varchar(10),
    overlay_public_id     text,
    overlay_version       bigint,
    overlay_format        varchar(10)
);

-- History page: WHERE user_id = ? ORDER BY created_at DESC
CREATE INDEX idx_history_user_created ON prediction_history (user_id, created_at DESC);

-- Password reset: WHERE reset_token_hash = ? (only rows that have a token)
CREATE INDEX idx_users_reset_token ON users (reset_token_hash) WHERE reset_token_hash IS NOT NULL;
