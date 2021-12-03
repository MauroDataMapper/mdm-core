-- Delete orphaned securable resource group roles for non-existent subscribed models
DO
$$
    DECLARE
        subscribed_model_table_exists BOOLEAN := (SELECT TO_REGCLASS('federation.subscribed_model') IS NOT NULL);

    BEGIN
        IF subscribed_model_table_exists THEN
            DELETE
            FROM security.securable_resource_group_role
            WHERE securable_resource_domain_type = 'SubscribedModel' AND
                  securable_resource_id NOT IN (SELECT id FROM federation.subscribed_model);
        END IF;
    END;
$$;