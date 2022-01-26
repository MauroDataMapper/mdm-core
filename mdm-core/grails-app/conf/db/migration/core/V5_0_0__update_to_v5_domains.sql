ALTER TABLE core.api_property
    ADD path TEXT;
UPDATE core.api_property
SET path = CONCAT('api:', category, '.', key);
ALTER TABLE core.api_property
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.authority
    ADD path TEXT;
UPDATE core.authority
SET path = CONCAT('auth:', label, '@', url);
ALTER TABLE core.authority
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.breadcrumb_tree
    ADD path TEXT;
--TODO NEED TO SET NOT NULL AFTER java migration

ALTER TABLE core.metadata
    ADD path TEXT;
UPDATE core.metadata
SET path = CONCAT('md:', namespace, '.', key);
ALTER TABLE core.metadata
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.rule
    ADD path TEXT;
UPDATE core.rule
SET path = CONCAT('ru:', name);
ALTER TABLE core.rule
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.edit
    ADD path TEXT;
UPDATE core.edit
SET path = CONCAT('ed:', title);
ALTER TABLE core.edit
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.reference_file
    ADD path TEXT;
UPDATE core.reference_file
SET path = CONCAT('rf:', file_name);
ALTER TABLE core.reference_file
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.rule_representation
    ADD path TEXT;
UPDATE core.rule_representation
SET path = CONCAT('rr:', language);
ALTER TABLE core.rule_representation
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.semantic_link
    ADD path TEXT;
UPDATE core.semantic_link
SET path = CONCAT('sl:', link_type, '.', target_multi_facet_aware_item_domain_type, '.', target_multi_facet_aware_item_id);
ALTER TABLE core.semantic_link
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.user_image_file
    ADD path TEXT;
UPDATE core.user_image_file
SET path = CONCAT('uif:', file_name);
ALTER TABLE core.user_image_file
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE core.version_link
    ADD path TEXT;
UPDATE core.version_link
SET path = CONCAT('vl:', link_type, '.', target_model_domain_type, '.', target_model_id);
ALTER TABLE core.version_link
    ALTER COLUMN path SET NOT NULL;