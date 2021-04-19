ALTER TABLE referencedata.reference_summary_metadata
    RENAME COLUMN catalogue_item_domain_type TO multi_facet_aware_item_domain_type;
ALTER TABLE referencedata.reference_summary_metadata
    RENAME COLUMN catalogue_item_id TO multi_facet_aware_item_id;

