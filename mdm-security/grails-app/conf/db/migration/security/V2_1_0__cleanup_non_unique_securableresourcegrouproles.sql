-- Migrated data and instances which have had certain actions performed may have non-unique SRGRs
-- Fortunately we can delete these, choosing the Container_Admin over Editor over Reader roles and then arbitrarily the date created
WITH dups   AS (SELECT securable_resource_id,
                       user_group_id
                FROM maurodatamapper.security.securable_resource_group_role
                GROUP BY securable_resource_id,
                         user_group_id
                HAVING COUNT(*) > 1),
     ranked AS (
         SELECT srgr.id,
                srgr.securable_resource_id,
                srgr.user_group_id,
                srgr.date_created,
                srgr.securable_resource_domain_type,
                srgr.group_role_id,
                gr.display_name,
                ROW_NUMBER() OVER (PARTITION BY dups.securable_resource_id, dups.user_group_id ORDER BY display_name, gr.date_created) AS rank
         FROM maurodatamapper.security.securable_resource_group_role srgr
              INNER JOIN dups ON dups.securable_resource_id = srgr.securable_resource_id AND dups.user_group_id = srgr.user_group_id
              INNER JOIN maurodatamapper.security.group_role gr ON gr.id = srgr.group_role_id)
DELETE
FROM maurodatamapper.security.securable_resource_group_role
WHERE id IN (SELECT id
             FROM ranked);