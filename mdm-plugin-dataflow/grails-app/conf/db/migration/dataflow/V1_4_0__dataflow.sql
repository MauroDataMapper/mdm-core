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

CREATE SCHEMA IF NOT EXISTS dataflow;

CREATE TABLE dataflow.data_class_component (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    data_flow_id       UUID         NOT NULL,
    definition         TEXT,
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
CREATE TABLE dataflow.data_element_component (
    id                      UUID         NOT NULL,
    version                 INT8         NOT NULL,
    date_created            TIMESTAMP    NOT NULL,
    data_class_component_id UUID         NOT NULL,
    definition              TEXT,
    last_updated            TIMESTAMP    NOT NULL,
    path                    TEXT         NOT NULL,
    depth                   INT4         NOT NULL,
    breadcrumb_tree_id      UUID         NOT NULL,
    idx                     INT4         NOT NULL,
    created_by              VARCHAR(255) NOT NULL,
    aliases_string          TEXT,
    label                   TEXT         NOT NULL,
    description             TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE dataflow.data_flow (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    definition         TEXT,
    diagram_layout     TEXT,
    last_updated       TIMESTAMP    NOT NULL,
    path               TEXT         NOT NULL,
    depth              INT4         NOT NULL,
    source_id          UUID         NOT NULL,
    breadcrumb_tree_id UUID         NOT NULL,
    target_id          UUID         NOT NULL,
    idx                INT4         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    aliases_string     TEXT,
    label              TEXT         NOT NULL,
    description        TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE dataflow.join_data_class_component_to_source_data_class (
    data_class_component_id UUID NOT NULL,
    data_class_id           UUID
);
CREATE TABLE dataflow.join_data_class_component_to_target_data_class (
    data_class_component_id UUID NOT NULL,
    data_class_id           UUID
);
CREATE TABLE dataflow.join_data_element_component_to_source_data_element (
    data_element_component_id UUID NOT NULL,
    data_element_id           UUID
);
CREATE TABLE dataflow.join_data_element_component_to_target_data_element (
    data_element_component_id UUID NOT NULL,
    data_element_id           UUID
);
CREATE TABLE dataflow.join_dataClassComponent_to_facet (
    dataClassComponent_id UUID NOT NULL,
    classifier_id         UUID,
    annotation_id         UUID,
    semantic_link_id      UUID,
    reference_file_id     UUID,
    metadata_id           UUID
);
CREATE TABLE dataflow.join_dataElementComponent_to_facet (
    dataElementComponent_id UUID NOT NULL,
    classifier_id           UUID,
    annotation_id           UUID,
    semantic_link_id        UUID,
    reference_file_id       UUID,
    metadata_id             UUID
);
CREATE TABLE dataflow.join_dataFlow_to_facet (
    dataFlow_id       UUID NOT NULL,
    classifier_id     UUID,
    annotation_id     UUID,
    semantic_link_id  UUID,
    reference_file_id UUID,
    metadata_id       UUID
);
CREATE INDEX dataClassComponent_created_by_idx ON dataflow.data_class_component(created_by);
CREATE INDEX dataElementComponent_created_by_idx ON dataflow.data_element_component(created_by);
CREATE INDEX data_flow_source_idx ON dataflow.data_flow(source_id);
CREATE INDEX data_flow_target_idx ON dataflow.data_flow(target_id);
CREATE INDEX dataFlow_created_by_idx ON dataflow.data_flow(created_by);

ALTER TABLE IF EXISTS dataflow.data_class_component
    ADD CONSTRAINT FK8qu1p2ejn32fxvwbtqmcb28d4 FOREIGN KEY (data_flow_id) REFERENCES dataflow.data_flow;
ALTER TABLE IF EXISTS dataflow.data_class_component
    ADD CONSTRAINT FKevgs9u7n7x5tr0a32ce3br9pi FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS dataflow.data_element_component
    ADD CONSTRAINT FK8q670e83q94a20x8urckoqhs7 FOREIGN KEY (data_class_component_id) REFERENCES dataflow.data_class_component;
ALTER TABLE IF EXISTS dataflow.data_element_component
    ADD CONSTRAINT FKpfgnmog9cl0w1lmqoor55xq3p FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS dataflow.data_flow
    ADD CONSTRAINT FK3fj19l4nvknojy3srxmkdfw5w FOREIGN KEY (source_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS dataflow.data_flow
    ADD CONSTRAINT FK77hjma5cdtsc07lk9axb9uplj FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS dataflow.data_flow
    ADD CONSTRAINT FKl8uawgeg58jq51ydqqddm5d7g FOREIGN KEY (target_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS dataflow.join_data_class_component_to_source_data_class
    ADD CONSTRAINT FK8rlgnf224u6byjb9mutxvj02d FOREIGN KEY (data_class_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS dataflow.join_data_class_component_to_source_data_class
    ADD CONSTRAINT FK69j2bufggb1whkshma276fb3u FOREIGN KEY (data_class_component_id) REFERENCES dataflow.data_class_component;
ALTER TABLE IF EXISTS dataflow.join_data_class_component_to_target_data_class
    ADD CONSTRAINT FKjp8k503bbqqe4h6s0f7uygg8n FOREIGN KEY (data_class_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS dataflow.join_data_class_component_to_target_data_class
    ADD CONSTRAINT FK5n8do09dd74fa9h1n73ovvule FOREIGN KEY (data_class_component_id) REFERENCES dataflow.data_class_component;
ALTER TABLE IF EXISTS dataflow.join_data_element_component_to_source_data_element
    ADD CONSTRAINT FKnmiwa6fd5ohwd00f0sk0wfx3t FOREIGN KEY (data_element_id) REFERENCES datamodel.data_element;
ALTER TABLE IF EXISTS dataflow.join_data_element_component_to_source_data_element
    ADD CONSTRAINT FKfj2dcm6f4pug84c27slqx72sb FOREIGN KEY (data_element_component_id) REFERENCES dataflow.data_element_component;
ALTER TABLE IF EXISTS dataflow.join_data_element_component_to_target_data_element
    ADD CONSTRAINT FK75eg0xy6obhx83sahuf43ftkn FOREIGN KEY (data_element_id) REFERENCES datamodel.data_element;
ALTER TABLE IF EXISTS dataflow.join_data_element_component_to_target_data_element
    ADD CONSTRAINT FKo677lt6vljfo4mcjbhn0y4bf6 FOREIGN KEY (data_element_component_id) REFERENCES dataflow.data_element_component;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FKi22diqv42nnrxmhyki9f8sodi FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FK9nd41ujgegfisr6s7prcxle75 FOREIGN KEY (dataClassComponent_id) REFERENCES dataflow.data_class_component;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FKjnu3epst826kd40f60ktimo6k FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FK83mqbv5ca5sjld100rbiymsvs FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FKe3nbbi9b4igb936kcxlx9lcxd FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS dataflow.join_dataClassComponent_to_facet
    ADD CONSTRAINT FKmfkn6if9k5q1k938jr5mx2lhw FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FKnf8wevvjjhglny27yn1yoav83 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FKbcxohbk6botm68gguiqulgveq FOREIGN KEY (dataElementComponent_id) REFERENCES dataflow.data_element_component;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FKj8cv4bqtulig1rg7f0xikfr2d FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FKrs6wh8ahehpma0s81ysqruvgp FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FK2458d1q7dlb53wk3i2f3tvn07 FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS dataflow.join_dataElementComponent_to_facet
    ADD CONSTRAINT FK6oar34bhid29tojvm1ukllq7t FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FKpwwfp2jwv5f5kwascasa113r1 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FK4lftftotgkhj732e3cdofnua9 FOREIGN KEY (dataFlow_id) REFERENCES dataflow.data_flow;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FK6i15t337ti18ejj9g11ntw7wa FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FK3desra9ff6a5m317j5emcbrb FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FK18w0v8pjw1ejcppns1ovsaiuh FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS dataflow.join_dataFlow_to_facet
    ADD CONSTRAINT FKpvp8i5ner679uom2d32bu59f7 FOREIGN KEY (metadata_id) REFERENCES core.metadata;
