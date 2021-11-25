-- Delete orphaned securable resource group roles for non-existent subscribed models
DELETE
FROM security.securable_resource_group_role
WHERE securable_resource_domain_type = 'SubscribedModel' AND
      securable_resource_id NOT IN (SELECT id FROM federation.subscribed_model);