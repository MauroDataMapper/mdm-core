-- If the core tables still exist then drop them
DO
$$
    DECLARE
        model_extend_exists BOOLEAN := (SELECT TO_REGCLASS('core.model_extend') IS NOT NULL);
        DECLARE
        model_import_exists BOOLEAN := (SELECT TO_REGCLASS('core.model_import') IS NOT NULL);

    BEGIN
        IF model_extend_exists = TRUE
        THEN
            ALTER TABLE datamodel.join_dataclass_to_facet
                DROP CONSTRAINT fkdataclass_to_model_extend;
            DROP TABLE core.model_extend;
        END IF;
        IF model_import_exists = TRUE
        THEN
            ALTER TABLE datamodel.join_dataclass_to_facet
                DROP CONSTRAINT fkdataclass_to_model_import;
            ALTER TABLE datamodel.join_datamodel_to_facet
                DROP CONSTRAINT fkdatamodel_to_model_import;
            DROP TABLE core.model_import CASCADE;
        END IF;

    END;
$$;
;

ALTER TABLE datamodel.join_datamodel_to_facet
    DROP COLUMN model_import_id;

ALTER TABLE datamodel.join_dataclass_to_facet
    DROP COLUMN model_import_id;
ALTER TABLE datamodel.join_dataclass_to_facet
    DROP COLUMN model_extend_id;

