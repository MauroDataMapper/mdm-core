ALTER TABLE core.annotation
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.annotation
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

ALTER TABLE core.metadata
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.metadata
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

ALTER TABLE core.reference_file
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.reference_file
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

ALTER TABLE core.rule
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.rule
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

ALTER TABLE core.semantic_link
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.semantic_link
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;
ALTER TABLE core.semantic_link
    RENAME COLUMN target_catalogue_item_domain_type TO target_multi_facet_aware_item_domain_type;
ALTER TABLE core.semantic_link
    RENAME COLUMN target_catalogue_item_id TO target_multi_facet_aware_item_id;

ALTER TABLE core.version_link
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE core.version_link
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;