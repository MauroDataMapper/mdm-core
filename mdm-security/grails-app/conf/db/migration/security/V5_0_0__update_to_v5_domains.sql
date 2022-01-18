ALTER TABLE security.catalogue_user
    ADD path TEXT NOT NULL;

ALTER TABLE security.user_group
    ADD path TEXT NOT NULL;

ALTER TABLE security.securable_resource_group_role
    ADD path TEXT;

ALTER TABLE security.api_key
    ADD path TEXT;
