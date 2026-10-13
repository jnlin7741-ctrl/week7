CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_app_user_email UNIQUE (email)
);
CREATE TABLE post (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    title VARCHAR(120) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
CREATE INDEX ix_post_created_at ON post (created_at DESC);
CREATE INDEX ix_post_owner_id ON post (owner_id);
