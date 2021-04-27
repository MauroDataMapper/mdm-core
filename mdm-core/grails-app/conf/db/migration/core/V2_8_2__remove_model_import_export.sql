DO
$$
    DECLARE
        datamodel_schema_exists BOOLEAN := (SELECT TO_REGCLASS('datamodel.flyway_schema_history') IS NOT NULL);

    BEGIN
        -- If no datamodel schema then we can drop the tables at this point
        -- If datamodel schema then we will need to affect these changes in the datamodel migration
        IF datamodel_schema_exists = FALSE
        THEN
            DROP TABLE core.model_extend CASCADE;
            DROP TABLE core.model_import CASCADE;
        END IF;


    END;
$$;
;