alter table core.join_classifier_to_facet
      add
      annotation_id uuid,
      rule_id uuid,
      semantic_link_id uuid,
      reference_file_id uuid,
      metadata_id uuid;
alter table core.join_folder_to_facet
    add
    annotation_id uuid,
    rule_id uuid,
    semantic_link_id uuid,
    reference_file_id uuid,
    metadata_id uuid;


alter table if exists core.join_classifier_to_facet
    add constraint FK3h1hax9omk9o62119jsc45m35 foreign key (annotation_id) references core.annotation;
alter table if exists core.join_classifier_to_facet
    add constraint FK54j6lhkhnneag9rqsnchk9rwf foreign key (classifier_id) references core.classifier;
alter table if exists core.join_classifier_to_facet
    add constraint FK1yihq7q1hhwm3f7jn4g7isg5k foreign key (rule_id) references core.rule;
alter table if exists core.join_classifier_to_facet
    add constraint FKs9xsugq08k5ejrfha2540ups0 foreign key (semantic_link_id) references core.semantic_link;
alter table if exists core.join_classifier_to_facet
    add constraint FK5owmrlff8c3f3bf2e7om5xkfj foreign key (reference_file_id) references core.reference_file;
alter table if exists core.join_classifier_to_facet
    add constraint FK6531dcod746lwh2v7k4fatx7b foreign key (metadata_id) references core.metadata;
alter table if exists core.join_folder_to_facet
    add constraint FKohkkmadsw0xtk5qs2mx0y0npo foreign key (annotation_id) references core.annotation;
alter table if exists core.join_folder_to_facet
    add constraint FKibq4i08l0b0nkbopm8wjrdfd9 foreign key (folder_id) references core.folder;
alter table if exists core.join_folder_to_facet
    add constraint FKml4kb6cf0wr79sopbu6fglets foreign key (rule_id) references core.rule;
alter table if exists core.join_folder_to_facet
    add constraint FKsuj7eo7stfn56f1b0ci16uqc4 foreign key (semantic_link_id) references core.semantic_link;
alter table if exists core.join_folder_to_facet
    add constraint FK6bgvwj5n9a92tkoky84uaktlm foreign key (reference_file_id) references core.reference_file;
alter table if exists core.join_folder_to_facet
    add constraint FK14o06qtiem74ycw6896javux7 foreign key (metadata_id) references core.metadata;
