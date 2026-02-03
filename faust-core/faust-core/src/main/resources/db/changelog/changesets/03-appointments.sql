-- liquibase formatted sql
-- changeset gemini:03-appointments

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    person_id BIGINT NOT NULL,
    occupation_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_acting BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_note VARCHAR(2000),

    -- Foreign Key constraints ensure data integrity
    CONSTRAINT fk_appointment_person
        FOREIGN KEY (person_id)
        REFERENCES persons(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_appointment_occupation
        FOREIGN KEY (occupation_id)
        REFERENCES occupations(id)
        ON DELETE CASCADE
);

-- Index for faster lookups when building the tree
CREATE INDEX idx_appointments_occupation ON appointments(occupation_id);
