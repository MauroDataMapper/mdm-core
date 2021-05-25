/*
 Terminologies
 */
-- Simple one, just update the link type to the correct fork type where the labels dont match
WITH data AS (
    SELECT vl.id AS vl_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.terminology source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.terminology target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'NEW_MODEL_VERSION_OF' AND
          source.label <> target.label
)
UPDATE maurodatamapper.core.version_link vl
SET link_type = 'NEW_FORK_OF'
FROM data
WHERE vl.id = data.vl_id;

-- Complex one, we need to swop the direction of the link and remove SUPERSEDED_BY_MODEL
-- Swopping the data around is easy, we also need to alter the facet join table
WITH data AS (
    SELECT vl.id     AS vl_id,
           source.id AS new_target_id,
           target.id AS new_source_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.terminology source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.terminology target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'SUPERSEDED_BY_MODEL'
)
UPDATE maurodatamapper.core.version_link vl
SET link_type                 = 'NEW_FORK_OF',
    target_model_id           = new_target_id,
    multi_facet_aware_item_id = new_source_id
FROM data
WHERE data.vl_id = vl.id
;

-- Make sure the facet table is connected to the new source
WITH data AS (
    SELECT vl.id                        AS vl_id,
           vl.multi_facet_aware_item_id AS new_source_id
    FROM maurodatamapper.terminology.join_terminology_to_facet jt
         INNER JOIN maurodatamapper.core.version_link vl ON jt.version_link_id = vl.id
    WHERE vl.link_type = 'NEW_FORK_OF' AND
          jt.terminology_id <> vl.multi_facet_aware_item_id
)
UPDATE maurodatamapper.terminology.join_terminology_to_facet jt
SET terminology_id = data.new_source_id
FROM data
WHERE jt.terminology_id <> new_source_id AND
      jt.version_link_id = data.vl_id;

-- Now remove the deprecated SUPERSEDED_BY_DOCUMENTATION
-- Complex one, we need to swop the direction of the link
-- Swopping the data around is easy, we also need to alter the facet join table
WITH data AS (
    SELECT vl.id     AS vl_id,
           source.id AS new_target_id,
           target.id AS new_source_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.terminology source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.terminology target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'SUPERSEDED_BY_DOCUMENTATION'
)
UPDATE maurodatamapper.core.version_link vl
SET link_type                 = 'NEW_DOCUMENTATION_VERSION_OF',
    target_model_id           = new_target_id,
    multi_facet_aware_item_id = new_source_id
FROM data
WHERE data.vl_id = vl.id
;

-- Make sure the facet table is connected to the new source
WITH data AS (
    SELECT vl.id                        AS vl_id,
           vl.multi_facet_aware_item_id AS new_source_id
    FROM maurodatamapper.terminology.join_terminology_to_facet jt
         INNER JOIN maurodatamapper.core.version_link vl ON jt.version_link_id = vl.id
    WHERE vl.link_type = 'NEW_DOCUMENTATION_VERSION_OF' AND
          jt.terminology_id <> vl.multi_facet_aware_item_id
)
UPDATE maurodatamapper.terminology.join_terminology_to_facet jt
SET terminology_id = data.new_source_id
FROM data
WHERE jt.terminology_id <> new_source_id AND
      jt.version_link_id = data.vl_id;
/*
 CodeSets
 */
-- Simple one, just update the link type to the correct fork type where the labels dont match
WITH data AS (
    SELECT vl.id AS vl_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.code_set source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.code_set target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'NEW_MODEL_VERSION_OF' AND
          source.label <> target.label
)
UPDATE maurodatamapper.core.version_link vl
SET link_type = 'NEW_FORK_OF'
FROM data
WHERE vl.id = data.vl_id;

-- Complex one, we need to swop the direction of the link and remove SUPERSEDED_BY_MODEL
-- Swopping the data around is easy, we also need to alter the facet join table
WITH data AS (
    SELECT vl.id     AS vl_id,
           source.id AS new_target_id,
           target.id AS new_source_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.code_set source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.code_set target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'SUPERSEDED_BY_MODEL'
)
UPDATE maurodatamapper.core.version_link vl
SET link_type                 = 'NEW_FORK_OF',
    target_model_id           = new_target_id,
    multi_facet_aware_item_id = new_source_id
FROM data
WHERE data.vl_id = vl.id
;

-- Make sure the facet table is connected to the new source
WITH data AS (
    SELECT vl.id                        AS vl_id,
           vl.multi_facet_aware_item_id AS new_source_id
    FROM maurodatamapper.terminology.join_codeset_to_facet jt
         INNER JOIN maurodatamapper.core.version_link vl ON jt.version_link_id = vl.id
    WHERE vl.link_type = 'NEW_FORK_OF' AND
          jt.codeset_id <> vl.multi_facet_aware_item_id
)
UPDATE maurodatamapper.terminology.join_codeset_to_facet jt
SET codeset_id = data.new_source_id
FROM data
WHERE jt.codeset_id <> new_source_id AND
      jt.version_link_id = data.vl_id;

-- Now remove the deprecated SUPERSEDED_BY_DOCUMENTATION
-- Complex one, we need to swop the direction of the link
-- Swopping the data around is easy, we also need to alter the facet join table
WITH data AS (
    SELECT vl.id     AS vl_id,
           source.id AS new_target_id,
           target.id AS new_source_id
    FROM maurodatamapper.core.version_link vl
         LEFT JOIN maurodatamapper.terminology.code_set source ON source.id = vl.multi_facet_aware_item_id AND vl.multi_facet_aware_item_domain_type = 'DataModel'
         LEFT JOIN maurodatamapper.terminology.code_set target ON target.id = vl.target_model_id AND vl.target_model_domain_type = 'DataModel'
    WHERE link_type = 'SUPERSEDED_BY_DOCUMENTATION'
)
UPDATE maurodatamapper.core.version_link vl
SET link_type                 = 'NEW_DOCUMENTATION_VERSION_OF',
    target_model_id           = new_target_id,
    multi_facet_aware_item_id = new_source_id
FROM data
WHERE data.vl_id = vl.id
;

-- Make sure the facet table is connected to the new source
WITH data AS (
    SELECT vl.id                        AS vl_id,
           vl.multi_facet_aware_item_id AS new_source_id
    FROM maurodatamapper.terminology.join_codeset_to_facet jt
         INNER JOIN maurodatamapper.core.version_link vl ON jt.version_link_id = vl.id
    WHERE vl.link_type = 'NEW_DOCUMENTATION_VERSION_OF' AND
          jt.codeset_id <> vl.multi_facet_aware_item_id
)
UPDATE maurodatamapper.terminology.join_codeset_to_facet jt
SET codeset_id = data.new_source_id
FROM data
WHERE jt.codeset_id <> new_source_id AND
      jt.version_link_id = data.vl_id;