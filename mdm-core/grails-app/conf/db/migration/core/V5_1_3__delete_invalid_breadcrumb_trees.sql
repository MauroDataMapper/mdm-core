-- before mdm-core version 5.2.0, child BreadcrumbTrees were not deleted when parent models were deleted
-- clean up orphaned BreadcrumbTrees

WITH RECURSIVE valid_breadcrumb_trees AS MATERIALIZED (
    SELECT id,
           parent_id
    FROM core.breadcrumb_tree
    WHERE top_breadcrumb_tree = TRUE
    UNION ALL
    SELECT c.id,
           c.parent_id
    FROM core.breadcrumb_tree c
         INNER JOIN valid_breadcrumb_trees v
                    ON c.parent_id = v.id
)
DELETE
FROM core.breadcrumb_tree c
WHERE NOT EXISTS(
        SELECT NULL
        FROM valid_breadcrumb_trees v
        WHERE c.id = v.id
    );