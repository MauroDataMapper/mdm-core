ALTER TABLE security.securable_resource_group_role
    ADD can_finalise_model BOOLEAN;

-- Finalised models cant be finalised so we can set that
UPDATE security.securable_resource_group_role
SET can_finalise_model = FALSE
WHERE finalised_model IS NOT NULL AND
      finalised_model = TRUE