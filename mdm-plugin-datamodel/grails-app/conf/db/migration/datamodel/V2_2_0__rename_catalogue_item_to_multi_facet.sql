ALTER TABLE datamodel.summary_metadata
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE datamodel.summary_metadata
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

