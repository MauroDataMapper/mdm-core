CREATE SCHEMA IF NOT EXISTS migration;

CREATE TABLE migration.database_metadata (
    id                                 UUID         NOT NULL PRIMARY KEY,
    version                            BIGINT       NOT NULL,
    date_created                       TIMESTAMP    NOT NULL,
    last_updated                       TIMESTAMP    NOT NULL,
    multi_facet_aware_item_domain_type VARCHAR(255) NOT NULL,
    namespace                          TEXT         NOT NULL,
    multi_facet_aware_item_id          UUID,
    value                              TEXT         NOT NULL,
    created_by                         VARCHAR(255) NOT NULL,
    key                                TEXT         NOT NULL
);

CREATE TABLE migration.additional_metadata (
    id                                 UUID         NOT NULL PRIMARY KEY,
    version                            BIGINT       NOT NULL,
    date_created                       TIMESTAMP    NOT NULL,
    last_updated                       TIMESTAMP    NOT NULL,
    multi_facet_aware_item_domain_type VARCHAR(255) NOT NULL,
    namespace                          TEXT         NOT NULL,
    multi_facet_aware_item_id          UUID,
    value                              TEXT         NOT NULL,
    created_by                         VARCHAR(255) NOT NULL,
    key                                TEXT         NOT NULL
);


-- Extract all metadata for databases for speed
INSERT INTO migration.database_metadata
SELECT *
FROM core.metadata
WHERE namespace LIKE 'ox.softeng.metadatacatalogue.plugins.database%' AND
      "key" LIKE '%[%]%';

-- Extract out the name of MD like 'foreign_key[dhsjdhs]'
INSERT
INTO migration.additional_metadata(id, version, date_created, last_updated, multi_facet_aware_item_domain_type, namespace, multi_facet_aware_item_id, "value", created_by,
                                   "key")
SELECT uuid_generate_v1(),
       version,
       date_created,
       last_updated,
       multi_facet_aware_item_domain_type,
       REGEXP_REPLACE(namespace, '^ox.softeng.metadatacatalogue.plugins.database', 'uk.ac.ox.softeng.metadatacatalogue.plugins.database'),
       multi_facet_aware_item_id,
       SUBSTRING("key", '.+?\[(.+)\]'),
       created_by,
       CONCAT(SUBSTRING("key", '(.+?)\[.+\]'), '_name')
FROM migration.database_metadata;

-- Migrate the namespace
UPDATE core.metadata
SET namespace = CONCAT('uk.ac.', namespace)
WHERE namespace LIKE 'ox.softeng.metadatacatalogue.plugins.database%';

-- Remove the constraint name from the existing key values in metadata
UPDATE core.metadata
SET "key"= CONCAT(SUBSTRING("key", '(.+?)\[.+\]'), '_columns')
WHERE namespace LIKE 'uk.ac.ox.softeng.metadatacatalogue.plugins.database%' AND
      "key" LIKE '%[%]%';

-- Insert new rows
INSERT INTO core.metadata(id, version, date_created, last_updated, multi_facet_aware_item_domain_type, namespace, multi_facet_aware_item_id, "value", created_by, "key")
SELECT id,
       version,
       date_created,
       last_updated,
       multi_facet_aware_item_domain_type,
       namespace,
       multi_facet_aware_item_id,
       "value",
       created_by,
       "key"
FROM migration.additional_metadata;

-- Add the new rows to the facet join tables
-- Only databases are imported like this
INSERT INTO datamodel.join_datamodel_to_facet(datamodel_id, metadata_id)
SELECT multi_facet_aware_item_id,
       id
FROM migration.additional_metadata
WHERE multi_facet_aware_item_domain_type = 'DataModel';

INSERT INTO datamodel.join_dataclass_to_facet(dataclass_id, metadata_id)
SELECT multi_facet_aware_item_id,
       id
FROM migration.additional_metadata
WHERE multi_facet_aware_item_domain_type = 'DataClass';

INSERT INTO datamodel.join_dataelement_to_facet(dataelement_id, metadata_id)
SELECT multi_facet_aware_item_id,
       id
FROM migration.additional_metadata
WHERE multi_facet_aware_item_domain_type = 'DataElement';

INSERT INTO datamodel.join_datatype_to_facet(datatype_id, metadata_id)
SELECT multi_facet_aware_item_id,
       id
FROM migration.additional_metadata
WHERE multi_facet_aware_item_domain_type IN ('PrimitiveType', 'ReferenceType', 'EnumerationType');

INSERT INTO datamodel.join_enumerationvalue_to_facet(enumerationvalue_id, metadata_id)
SELECT multi_facet_aware_item_id,
       id
FROM migration.additional_metadata
WHERE multi_facet_aware_item_domain_type = 'EnumerationValue';

-- Cleanup
DROP TABLE migration.database_metadata;
DROP TABLE migration.additional_metadata;
