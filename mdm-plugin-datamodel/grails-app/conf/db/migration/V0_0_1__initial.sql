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

-- grails schema-export grails-app/conf/db/migration/V0_0_1__initial.sql

CREATE TABLE datamodel.data_class (
    id                   UUID         NOT NULL,
    version              INT8         NOT NULL,
    date_created         TIMESTAMP    NOT NULL,
    last_updated         TIMESTAMP    NOT NULL,
    path                 TEXT         NOT NULL,
    depth                INT4         NOT NULL,
    min_multiplicity     INT4,
    max_multiplicity     INT4,
    parent_data_class_id UUID,
    breadcrumb_tree_id   UUID         NOT NULL,
    data_model_id        UUID         NOT NULL,
    idx                  INT4         NOT NULL,
    created_by           VARCHAR(255) NOT NULL,
    aliases_string       TEXT,
    label                TEXT         NOT NULL,
    description          TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE datamodel.data_element (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    data_class_id      UUID         NOT NULL,
    last_updated       TIMESTAMP    NOT NULL,
    path               TEXT         NOT NULL,
    depth              INT4         NOT NULL,
    min_multiplicity   INT4,
    max_multiplicity   INT4,
    breadcrumb_tree_id UUID         NOT NULL,
    data_type_id       UUID         NOT NULL,
    idx                INT4         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    aliases_string     TEXT,
    label              TEXT         NOT NULL,
    description        TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE datamodel.data_model (
    id                    UUID         NOT NULL,
    version               INT8         NOT NULL,
    date_created          TIMESTAMP    NOT NULL,
    finalised             BOOLEAN      NOT NULL,
    date_finalised        TIMESTAMP,
    documentation_version VARCHAR(255) NOT NULL,
    model_type            VARCHAR(255) NOT NULL,
    last_updated          TIMESTAMP    NOT NULL,
    organisation          VARCHAR(255),
    deleted               BOOLEAN      NOT NULL,
    author                VARCHAR(255),
    breadcrumb_tree_id    UUID         NOT NULL,
    folder_id             UUID         NOT NULL,
    created_by            VARCHAR(255) NOT NULL,
    aliases_string        TEXT,
    label                 TEXT         NOT NULL,
    description           TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE datamodel.data_type (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    domain_type        VARCHAR(15)  NOT NULL,
    last_updated       TIMESTAMP    NOT NULL,
    path               TEXT         NOT NULL,
    depth              INT4         NOT NULL,
    breadcrumb_tree_id UUID         NOT NULL,
    data_model_id      UUID         NOT NULL,
    idx                INT4         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    aliases_string     TEXT,
    label              TEXT         NOT NULL,
    description        TEXT,
    class              VARCHAR(255) NOT NULL,
    units              VARCHAR(255),
    reference_class_id UUID,
    PRIMARY KEY (id)
);
CREATE TABLE datamodel.enumeration_value (
    id                  UUID         NOT NULL,
    version             INT8         NOT NULL,
    date_created        TIMESTAMP    NOT NULL,
    enumeration_type_id UUID         NOT NULL,
    value               TEXT         NOT NULL,
    last_updated        TIMESTAMP    NOT NULL,
    path                TEXT         NOT NULL,
    depth               INT4         NOT NULL,
    breadcrumb_tree_id  UUID         NOT NULL,
    idx                 INT4         NOT NULL,
    category            TEXT,
    created_by          VARCHAR(255) NOT NULL,
    aliases_string      TEXT,
    key                 TEXT         NOT NULL,
    label               TEXT         NOT NULL,
    description         TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE datamodel.join_dataClass_to_facet (
    dataClass_id      UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE datamodel.join_dataElement_to_facet (
    dataElement_id    UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE datamodel.join_dataModel_to_facet (
    dataModel_id      UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    version_link_id   UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE datamodel.join_dataType_to_facet (
    dataType_id       UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE TABLE datamodel.join_enumerationValue_to_facet (
    enumerationValue_id UUID NOT NULL,
    classifier_id       UUID,
    annotation_id       UUID,
    semantic_link_id    UUID,
    reference_file_id   UUID,
    metadata_id         UUID
);
CREATE INDEX data_class_parent_data_class_idx ON datamodel.data_class(parent_data_class_id);
CREATE INDEX data_class_data_model_idx ON datamodel.data_class(data_model_id);
CREATE INDEX dataClass_created_by_idx ON datamodel.data_class(created_by);
CREATE INDEX data_element_data_class_idx ON datamodel.data_element(data_class_id);
CREATE INDEX data_element_data_type_idx ON datamodel.data_element(data_type_id);
CREATE INDEX dataElement_created_by_idx ON datamodel.data_element(created_by);
CREATE INDEX dataModel_created_by_idx ON datamodel.data_model(created_by);
CREATE INDEX data_type_data_model_idx ON datamodel.data_type(data_model_id);
CREATE INDEX dataType_created_by_idx ON datamodel.data_type(created_by);
CREATE INDEX reference_type_reference_class_idx ON datamodel.data_type(reference_class_id);
CREATE INDEX enumeration_value_enumeration_type_idx ON datamodel.enumeration_value(enumeration_type_id);
CREATE INDEX enumerationValue_created_by_idx ON datamodel.enumeration_value(created_by);
ALTER TABLE IF EXISTS core.annotation
    ADD CONSTRAINT FKnrnwt8d2s4kytg7mis2rg2a5x FOREIGN KEY (parent_annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS core.breadcrumb_tree
    ADD CONSTRAINT FK1hraqwgiiva4reb2v6do4it81 FOREIGN KEY (parent_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS core.classifier
    ADD CONSTRAINT FKahkm58kcer6a9q2v01ealovr6 FOREIGN KEY (parent_classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS core.folder
    ADD CONSTRAINT FK57g7veis1gp5wn3g0mp0x57pl FOREIGN KEY (parent_folder_id) REFERENCES core.folder;
ALTER TABLE IF EXISTS datamodel.data_class
    ADD CONSTRAINT FK71lrhqamsxh1b57sbigrgonq2 FOREIGN KEY (parent_data_class_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.data_class
    ADD CONSTRAINT FK4yr99q0xt49n31x48e78do1rq FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS datamodel.data_class
    ADD CONSTRAINT FK27usn28pto0b239mwltrfmksg FOREIGN KEY (data_model_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS datamodel.data_element
    ADD CONSTRAINT FK86to96ckvjf64qlwvosltcnsm FOREIGN KEY (data_class_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.data_element
    ADD CONSTRAINT FK6e7wo4o9bw27vk32roeo91cyn FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS datamodel.data_element
    ADD CONSTRAINT FKncb91jl5cylo6nmoolmkif0y4 FOREIGN KEY (data_type_id) REFERENCES datamodel.data_type;
ALTER TABLE IF EXISTS datamodel.data_model
    ADD CONSTRAINT FK9ybmrposbekl2h5pnwet4fx30 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS datamodel.data_model
    ADD CONSTRAINT FK5vqrag93xcmptnduomuj1d5up FOREIGN KEY (folder_id) REFERENCES core.folder;
ALTER TABLE IF EXISTS datamodel.data_type
    ADD CONSTRAINT FKsiu83nftgdvb7kdvaik9fghsj FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS datamodel.data_type
    ADD CONSTRAINT FKbqs2sknmwe6i3rtwrhflk9s5n FOREIGN KEY (data_model_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS datamodel.data_type
    ADD CONSTRAINT FKribr7hv9shypnj2iru0hsx2sn FOREIGN KEY (reference_class_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.enumeration_value
    ADD CONSTRAINT FKam3sx31p5a0eap02h4iu1nwsg FOREIGN KEY (enumeration_type_id) REFERENCES datamodel.data_type;
ALTER TABLE IF EXISTS datamodel.enumeration_value
    ADD CONSTRAINT FKj6s22vawbgx8qbi6u95umov5t FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FKgh9f6ok7n9wxwxopjku7yhfea FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FKc80l2pkf48a8sw4ijsudyaers FOREIGN KEY (dataClass_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FKpqpxtrqg9jh2ick2ug9mhcfxt FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FK7tq9mj4pasf5fmebs2sc9ap86 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FK5n6b907728hblnk0ihhwhbac4 FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS datamodel.join_dataClass_to_facet
    ADD CONSTRAINT FKewipna2xjervio2w9rsem7vvu FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FKdn8e1l2pofwmdpfroe9bkhskm FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FKpsyiacoeuww886wy5apt5idwq FOREIGN KEY (dataElement_id) REFERENCES datamodel.data_element;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FKe75uuv2w694ofrm1ogdqio495 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FK8roq23ibhwodnpibdp1srk6aq FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FK89immwtwlrbwrel10gjy3yimw FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS datamodel.join_dataElement_to_facet
    ADD CONSTRAINT FKg58co9t99dfp0076vkn23hemy FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FK1ek18e3t2cki6fch7jmbbati0 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FKb2bggjawxcb5pynsrnpwgw35q FOREIGN KEY (dataModel_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FK1yt7axbg37bynceoy6p06a5pk FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FKppqku5drbeh06ro6594sx7qpn FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FKk8m8u0b9dd216qsjdkbbttqmu FOREIGN KEY (version_link_id) REFERENCES core.version_link;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FKicjxoyym4mvpajl7amd2c96vg FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS datamodel.join_dataModel_to_facet
    ADD CONSTRAINT FKn8kvp5hpmtpu6t9ivldafifom FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FKq73nqfoqdhodobkio53xnoroj FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FKka92tyn95wh23p9y7rjb1sila FOREIGN KEY (dataType_id) REFERENCES datamodel.data_type;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FKs3obp3gh2qp7lvl7c2ke33672 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FKgfuqffr58ihdup07r1ys2rsts FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FKk6htfwfpc5ty1o1skmlw0ct5h FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS datamodel.join_dataType_to_facet
    ADD CONSTRAINT FK685o5rkte9js4kibmx3e201ul FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FKissxtxxag5rkhtjr2q1pivt64 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FKf8d99ketatffxmapoax1upmo8 FOREIGN KEY (enumerationValue_id) REFERENCES datamodel.enumeration_value;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FKso04vaqmba4n4ffdbx5gg0fly FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FKrefs16rh5cjm8rwngb9ijw9y1 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FK40tuyaalgpyfdnp2wqfl1bl3b FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS datamodel.join_enumerationValue_to_facet
    ADD CONSTRAINT FK9xuiuctli6j5hra8j0pw0xbib FOREIGN KEY (metadata_id) REFERENCES core.metadata;
