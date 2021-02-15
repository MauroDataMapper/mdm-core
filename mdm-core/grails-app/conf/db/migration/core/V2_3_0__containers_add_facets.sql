CREATE TABLE core.join_classifier_to_facet (
    classifier_id     UUID NOT NULL,
    annotation_id     UUID,
    rule_id           UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE core.join_folder_to_facet (
    folder_id         UUID NOT NULL,
    annotation_id     UUID,
    rule_id           UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);

ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FK3h1hax9omk9o62119jsc45m35 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FK54j6lhkhnneag9rqsnchk9rwf FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FK1yihq7q1hhwm3f7jn4g7isg5k FOREIGN KEY (rule_id) REFERENCES core.rule;
ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FKs9xsugq08k5ejrfha2540ups0 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FK5owmrlff8c3f3bf2e7om5xkfj FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS core.join_classifier_to_facet
    ADD CONSTRAINT FK6531dcod746lwh2v7k4fatx7b FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FKohkkmadsw0xtk5qs2mx0y0npo FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FKibq4i08l0b0nkbopm8wjrdfd9 FOREIGN KEY (folder_id) REFERENCES core.folder;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FKml4kb6cf0wr79sopbu6fglets FOREIGN KEY (rule_id) REFERENCES core.rule;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FKsuj7eo7stfn56f1b0ci16uqc4 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FK6bgvwj5n9a92tkoky84uaktlm FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS core.join_folder_to_facet
    ADD CONSTRAINT FK14o06qtiem74ycw6896javux7 FOREIGN KEY (metadata_id) REFERENCES core.metadata;