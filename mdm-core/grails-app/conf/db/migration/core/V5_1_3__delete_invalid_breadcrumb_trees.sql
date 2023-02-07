-- before mdm-core version 5.2.0, child BreadcrumbTrees were not deleted when parent models were deleted
-- clean up orphaned BreadcrumbTrees

CREATE TEMPORARY TABLE valid_breadcrumb_trees_temp AS
WITH RECURSIVE valid_breadcrumb_trees AS MATERIALIZED (
		SELECT *
		FROM core.breadcrumb_tree
		WHERE top_breadcrumb_tree = TRUE
		UNION ALL
		SELECT c.*
		FROM core.breadcrumb_tree c
			 INNER JOIN valid_breadcrumb_trees v
						ON c.parent_id = v.id
)

SELECT * FROM valid_breadcrumb_trees;

DO
$$
BEGIN
   IF EXISTS (SELECT aurora_version()) THEN
   	  TRUNCATE core.breadcrumb_tree CASCADE;
   ELSE
   	  ALTER TABLE core.breadcrumb_tree DISABLE TRIGGER ALL;
	  DELETE FROM core.breadcrumb_tree;
	  ALTER TABLE core.breadcrumb_tree ENABLE TRIGGER ALL;
   END IF;

   exception
	   when undefined_function then
			ALTER TABLE core.breadcrumb_tree DISABLE TRIGGER ALL;
		  	DELETE FROM core.breadcrumb_tree;
		  	ALTER TABLE core.breadcrumb_tree ENABLE TRIGGER ALL;
END
$$;

INSERT INTO core.breadcrumb_tree
SELECT * FROM valid_breadcrumb_trees_temp;

CREATE INDEX breadcrumb_tree_tree_string_idx ON core.breadcrumb_tree (tree_string text_pattern_ops);