ALTER TABLE core.api_property
    ADD path TEXT NOT NULL;

ALTER TABLE core.authority
    ADD path TEXT NOT NULL;

ALTER TABLE core.breadcrumb_tree
    ADD path TEXT NOT NULL;

ALTER TABLE core.metadata
    ADD path TEXT NOT NULL;

ALTER TABLE core.rule
    ADD path TEXT NOT NULL;

ALTER TABLE core.edit
    ADD path TEXT NOT NULL;

ALTER TABLE core.reference_file
    ADD path TEXT NOT NULL;

ALTER TABLE core.rule_representation
    ADD path TEXT NOT NULL;

ALTER TABLE core.semantic_link
    ADD path TEXT NOT NULL;

ALTER TABLE core.user_image_file
    ADD path TEXT NOT NULL;

ALTER TABLE core.version_link
    ADD path TEXT NOT NULL;