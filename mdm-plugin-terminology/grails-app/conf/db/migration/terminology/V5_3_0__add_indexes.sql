create index on terminology.terminology (label);
create index on terminology.terminology (branch_name);
create index on terminology.terminology (model_version);
create index on terminology.terminology (folder_id);

create index on terminology.code_set (label);
create index on terminology.code_set (branch_name);
create index on terminology.code_set (model_version);
create index on terminology.code_set (folder_id);

create index on terminology.term (path text_pattern_ops);
create index on terminology.term_relationship (path text_pattern_ops);

create index on terminology.term(breadcrumb_tree_id);
create index on terminology.term_relationship(breadcrumb_tree_id);