ALTER TABLE IF EXISTS  security.securable_resource_group_role
    ADD CONSTRAINT UKfa251ce1f3b24cad8cccd15394d1 UNIQUE (securable_resource_id, user_group_id);