ALTER TABLE security.catalogue_user
    ADD creation_method VARCHAR(255);

UPDATE security.catalogue_user
SET creation_method = 'Standard';

ALTER TABLE security.catalogue_user
    ALTER COLUMN creation_method SET NOT NULL;