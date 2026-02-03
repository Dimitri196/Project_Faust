-- liquibase formatted sql
-- changeset gemini:01-init-schema

CREATE TABLE persons (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    title_before VARCHAR(50),
    title_after VARCHAR(50),
    education_level VARCHAR(100),
    field_of_study VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(50),
    biography TEXT
);

CREATE TABLE occupations (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_vacant BOOLEAN DEFAULT TRUE
);