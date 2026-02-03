-- liquibase formatted sql
-- changeset dmitri:04-users-RETRY-V1
-- validCheckSum: any

CREATE TABLE users (
    user_id UUID NOT NULL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(255),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    clearance VARCHAR(50),
    status VARCHAR(50),
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user_tech_stack (
    user_id UUID NOT NULL,
    technology VARCHAR(255),
    CONSTRAINT fk_user_tech_stack_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);