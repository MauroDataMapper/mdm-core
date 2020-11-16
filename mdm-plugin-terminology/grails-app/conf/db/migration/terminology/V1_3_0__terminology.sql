--
-- Copyright 2020 University of Oxford
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
-- SPDX-License-Identifier: Apache-2.0
--

CREATE SCHEMA IF NOT EXISTS terminology;

CREATE TABLE terminology.code_set (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    finalised                       BOOLEAN      NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    date_finalised                  TIMESTAMP,
    documentation_version           VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    model_type                      VARCHAR(255) NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    organisation                    VARCHAR(255),
    deleted                         BOOLEAN      NOT NULL,
    author                          VARCHAR(255),
    breadcrumb_tree_id              UUID,
    folder_id                       UUID         NOT NULL,
    created_by                      VARCHAR(255) NOT NULL,
    aliases_string                  TEXT,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE terminology.join_codeSet_to_facet (
    codeSet_id        UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    version_link_id   UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE terminology.join_codeset_to_term (
    term_id    UUID NOT NULL,
    codeSet_id UUID NOT NULL
);
CREATE TABLE terminology.join_term_to_facet (
    term_id           UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE terminology.join_terminology_to_facet (
    terminology_id    UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    version_link_id   UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE terminology.join_termRelationship_to_facet (
    termRelationship_id UUID NOT NULL,
    classifier_id       UUID,
    annotation_id       UUID,
    semantic_link_id    UUID,
    reference_file_id   UUID,
    metadata_id         UUID
);
CREATE TABLE terminology.join_termRelationshipType_to_facet (
    termRelationshipType_id UUID NOT NULL,
    classifier_id           UUID,
    annotation_id           UUID,
    semantic_link_id        UUID,
    reference_file_id       UUID,
    metadata_id             UUID
);
CREATE TABLE terminology.term (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    url                VARCHAR(255),
    definition         TEXT         NOT NULL,
    terminology_id     UUID         NOT NULL,
    is_parent          BOOLEAN      NOT NULL,
    code               VARCHAR(255) NOT NULL,
    last_updated       TIMESTAMP    NOT NULL,
    path               TEXT         NOT NULL,
    depth              INT4         NOT NULL,
    breadcrumb_tree_id UUID         NOT NULL,
    idx                INT4         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    aliases_string     TEXT,
    label              TEXT         NOT NULL,
    description        TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE terminology.term_relationship (
    id                   UUID         NOT NULL,
    version              INT8         NOT NULL,
    date_created         TIMESTAMP    NOT NULL,
    target_term_id       UUID         NOT NULL,
    relationship_type_id UUID         NOT NULL,
    last_updated         TIMESTAMP    NOT NULL,
    path                 TEXT         NOT NULL,
    depth                INT4         NOT NULL,
    source_term_id       UUID         NOT NULL,
    breadcrumb_tree_id   UUID         NOT NULL,
    idx                  INT4         NOT NULL,
    created_by           VARCHAR(255) NOT NULL,
    aliases_string       TEXT,
    label                TEXT         NOT NULL,
    description          TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE terminology.term_relationship_type (
    id                    UUID         NOT NULL,
    version               INT8         NOT NULL,
    date_created          TIMESTAMP    NOT NULL,
    child_relationship    BOOLEAN      NOT NULL,
    terminology_id        UUID         NOT NULL,
    last_updated          TIMESTAMP    NOT NULL,
    path                  TEXT         NOT NULL,
    depth                 INT4         NOT NULL,
    breadcrumb_tree_id    UUID         NOT NULL,
    parental_relationship BOOLEAN      NOT NULL,
    idx                   INT4         NOT NULL,
    created_by            VARCHAR(255) NOT NULL,
    aliases_string        TEXT,
    label                 TEXT         NOT NULL,
    display_label         VARCHAR(255) NOT NULL,
    description           TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE terminology.terminology (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    finalised                       BOOLEAN      NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    date_finalised                  TIMESTAMP,
    documentation_version           VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    model_type                      VARCHAR(255) NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    organisation                    VARCHAR(255),
    deleted                         BOOLEAN      NOT NULL,
    author                          VARCHAR(255),
    breadcrumb_tree_id              UUID         NOT NULL,
    folder_id                       UUID         NOT NULL,
    created_by                      VARCHAR(255) NOT NULL,
    aliases_string                  TEXT,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX codeSet_created_by_idx ON terminology.code_set(created_by);
CREATE INDEX term_terminology_idx ON terminology.term(terminology_id);
CREATE INDEX term_created_by_idx ON terminology.term(created_by);
CREATE INDEX term_relationship_target_term_idx ON terminology.term_relationship(target_term_id);
CREATE INDEX term_relationship_source_term_idx ON terminology.term_relationship(source_term_id);
CREATE INDEX termRelationship_created_by_idx ON terminology.term_relationship(created_by);
CREATE INDEX term_relationship_type_terminology_idx ON terminology.term_relationship_type(terminology_id);
CREATE INDEX termRelationshipType_created_by_idx ON terminology.term_relationship_type(created_by);
CREATE INDEX terminology_created_by_idx ON terminology.terminology(created_by);
ALTER TABLE IF EXISTS terminology.code_set
    ADD CONSTRAINT FKfxs2u8sgiov5x5jf40oy3q2y3 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS terminology.code_set
    ADD CONSTRAINT FKp5k3i717iool706wniwjjvwv3 FOREIGN KEY (folder_id) REFERENCES core.folder;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FKis38oricalv28ssx3swcyfqe0 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FK7t7ilhhckw9qf6xrn1ubfm7d5 FOREIGN KEY (codeSet_id) REFERENCES terminology.code_set;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FK6cgrkxpermch26tfb07629so4 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FKopyxyabfcixr8q5p4tdfiatw FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FKf251q9vbfhi6t007drkr0ot56 FOREIGN KEY (version_link_id) REFERENCES core.version_link;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FKf977e6gh0go5gsb1mdypxq5qm FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS terminology.join_codeSet_to_facet
    ADD CONSTRAINT FKd6o1dmjdok9j9f4kk9kry3nlo FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS terminology.join_codeset_to_term
    ADD CONSTRAINT FKgova93e87cae5ibqn41b9i81q FOREIGN KEY (codeSet_id) REFERENCES terminology.code_set;
ALTER TABLE IF EXISTS terminology.join_codeset_to_term
    ADD CONSTRAINT FKrce6i901t3rmqwa7oh215fc99 FOREIGN KEY (term_id) REFERENCES terminology.term;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FK30th9e8a75qjf08804ttebhsm FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FK9bpl4j09xy1seyx3iaaueyapu FOREIGN KEY (term_id) REFERENCES terminology.term;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FKahmuw6nlc4rr8afxo7jw47wdf FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FK7jn78931gti2jluti9tm592p0 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FKs9timcfrvfej60b2b0pinlxs0 FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS terminology.join_term_to_facet
    ADD CONSTRAINT FKpvf7f5wddn60lwuucualmnfcu FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKsk9svegop687oy8527bni5mxl FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKt5xk7gkhiyj0y1snpsqhgwnhk FOREIGN KEY (terminology_id) REFERENCES terminology.terminology;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKi6m0bt3anil9c8xa1vkro2sex FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FK8rh0jwsnqbg5wj37sabpxt808 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKcu5iih9ugs9y5guu5mqwdymae FOREIGN KEY (version_link_id) REFERENCES core.version_link;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKmutj2dw99jmqoiyqs7elxax0b FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet
    ADD CONSTRAINT FKti72ejs5r77aweqn2voaukggw FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FKgx7mfxmfac6cjqhwfy8e0pema FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FK334wl2e2hfjm3641dvx9kbvrr FOREIGN KEY (termRelationship_id) REFERENCES terminology.term_relationship;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FK5nnjqhchac10vbq4dnturf43d FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FK9jq2jv72rf5xm5qvhw2808477 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FK7mj3h26tgnbprkogynq8ws1mx FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS terminology.join_termRelationship_to_facet
    ADD CONSTRAINT FKkejqseo866piupm5aos0tcewt FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FK3ampvxuqr5vc4wnpha04k33in FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FKimkg4xk0vgadayww633utts6m FOREIGN KEY (termRelationshipType_id) REFERENCES terminology.term_relationship_type;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FK6kxdv6f6gqa7xkm2bcywsohxy FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FK16s1q7crb8ipqjg55yc7mmjqm FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FK5w07m1k4c62vcduljr349h48j FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS terminology.join_termRelationshipType_to_facet
    ADD CONSTRAINT FK4p7n1lms874i479o632m3u0bc FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS terminology.term
    ADD CONSTRAINT FKcdm4c5ljr1inp380r0bsce94s FOREIGN KEY (terminology_id) REFERENCES terminology.terminology;
ALTER TABLE IF EXISTS terminology.term
    ADD CONSTRAINT FKpry3m6mjob704x9e0w56auich FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS terminology.term_relationship
    ADD CONSTRAINT FKd55uv21yk0qoax6ofaxbg5x9w FOREIGN KEY (target_term_id) REFERENCES terminology.term;
ALTER TABLE IF EXISTS terminology.term_relationship
    ADD CONSTRAINT FKpjx0bwxtjt6qewxak7fpgr0pk FOREIGN KEY (relationship_type_id) REFERENCES terminology.term_relationship_type;
ALTER TABLE IF EXISTS terminology.term_relationship
    ADD CONSTRAINT FKnaqfdwx75pqsv1x4yk4nopa8s FOREIGN KEY (source_term_id) REFERENCES terminology.term;
ALTER TABLE IF EXISTS terminology.term_relationship
    ADD CONSTRAINT FKa5wounncpjf0fcv4fpd12j10g FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS terminology.term_relationship_type
    ADD CONSTRAINT FKqlbqof0u5k91mxq16h2f1p2p8 FOREIGN KEY (terminology_id) REFERENCES terminology.terminology;
ALTER TABLE IF EXISTS terminology.term_relationship_type
    ADD CONSTRAINT FKksj1p00n2s6upo53rj0g2rcln FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS terminology.terminology
    ADD CONSTRAINT FKh0m1mr4fvlw79xuod2uffrvhx FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS terminology.terminology
    ADD CONSTRAINT FK8kiyjbnrjas88qosgt78fdue5 FOREIGN KEY (folder_id) REFERENCES core.folder;
