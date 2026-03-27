CREATE VIEW global_search_view AS

-- SEKTOR: PERSONS (Lidé)
SELECT 
    external_id AS id, 
    (last_name || ' ' || first_name) AS display_name, 
    'PERSON' AS category,
    political_affiliation AS sub_label,
    setweight(to_tsvector('simple', unaccent(COALESCE(last_name, ''))), 'A') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(first_name, ''))), 'B') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(political_affiliation, ''))), 'C') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(biography, ''))), 'D') AS search_vector
FROM persons

UNION ALL

-- SEKTOR: INSTITUTIONS (Instituce)
SELECT 
    external_id AS id, 
    name AS display_name, 
    'INSTITUTION' AS category,
    type AS sub_label, -- Zobrazí typ (např. vládní, soukromá) v HUDu
    setweight(to_tsvector('simple', unaccent(COALESCE(name, ''))), 'A') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(description, ''))), 'D') AS search_vector
FROM institutions

UNION ALL

-- SEKTOR: OCCUPATIONS (Pozice)
SELECT 
    external_id AS id, 
    title AS display_name, 
    'OCCUPATION' AS category,
    code AS sub_label, -- Zobrazí kód pozice v HUDu
    setweight(to_tsvector('simple', unaccent(COALESCE(title, ''))), 'A') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(code, ''))), 'B') ||
    setweight(to_tsvector('simple', unaccent(COALESCE(description, ''))), 'D') AS search_vector
FROM occupations;