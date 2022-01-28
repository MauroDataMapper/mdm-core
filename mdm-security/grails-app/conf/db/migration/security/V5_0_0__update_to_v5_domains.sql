ALTER TABLE security.catalogue_user
    ADD path TEXT;
UPDATE security.catalogue_user
SET path = CONCAT('cu:', email_address);
ALTER TABLE security.catalogue_user
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE security.user_group
    ADD path TEXT;
UPDATE security.user_group
SET path = CONCAT('ug:', name);
ALTER TABLE security.user_group
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE security.securable_resource_group_role
    ADD path TEXT;

ALTER TABLE security.api_key
    ADD path TEXT;
