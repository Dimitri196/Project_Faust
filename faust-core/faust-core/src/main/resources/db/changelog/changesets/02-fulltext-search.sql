-- liquibase formatted sql
-- changeset gemini:02-fulltext-search

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text AS $$
SELECT public.unaccent('public.unaccent', $1)
$$ LANGUAGE sql IMMUTABLE;

-- Přidání generovaného sloupce pro bleskové hledání bez diakritiky
ALTER TABLE persons
ADD COLUMN full_name_search_normalized TEXT
GENERATED ALWAYS AS (
    f_unaccent(lower(first_name || ' ' || last_name || ' ' || last_name || ' ' || first_name))
) STORED;

CREATE INDEX idx_persons_full_name_unaccent ON persons (full_name_search_normalized);