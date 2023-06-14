-- create index to speed up BreadcrumbTreeService::deleteAllByDomainIds
CREATE INDEX breadcrumb_tree_tree_string_prefix_idx ON core.breadcrumb_tree (SUBSTR(tree_string, 1, 36+1));
