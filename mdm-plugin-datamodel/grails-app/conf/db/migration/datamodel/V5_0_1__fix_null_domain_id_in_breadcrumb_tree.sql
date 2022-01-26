-- Doesnt happen in all instances but it exists in some, so this will just fix it across the board
UPDATE core.breadcrumb_tree bt
SET domain_id = ev.id
FROM datamodel.enumeration_value ev
WHERE ev.breadcrumb_tree_id = bt.id AND
      bt.domain_id IS NULL