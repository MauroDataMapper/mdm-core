DROP TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet;
   
ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN path;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN depth;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN breadcrumb_tree_id;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN idx;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN aliases_string;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN label;

ALTER TABLE IF EXISTS referencedata.reference_data_value
    DROP COLUMN description;                      
