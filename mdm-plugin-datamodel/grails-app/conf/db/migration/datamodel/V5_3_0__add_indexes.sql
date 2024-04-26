create index on datamodel.enumeration_value (path text_pattern_ops);
create index on datamodel.data_type (path text_pattern_ops);
create index on datamodel.data_class (path text_pattern_ops);
create index on datamodel.data_element (path text_pattern_ops);

create index on datamodel.data_class(breadcrumb_tree_id);
create index on datamodel.data_type(breadcrumb_tree_id);
create index on datamodel.data_element(breadcrumb_tree_id);
create index on datamodel.enumeration_value(breadcrumb_tree_id);

create index on datamodel.data_model (label);
create index on datamodel.data_model (branch_name);
create index on datamodel.data_model (model_version);