ALTER TABLE core.authority
    ADD default_authority BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE core.authority
SET default_authority               = TRUE,
    readable_by_authenticated_users = TRUE,
    readable_by_everyone            = TRUE
WHERE id = (SELECT id
            FROM core.authority
            ORDER BY date_created
            LIMIT 1)