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

CREATE SCHEMA IF NOT EXISTS referencedatamodel;

CREATE TABLE referencedatamodel.reference_data_model (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    finalised                       BOOLEAN      NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    date_finalised                  TIMESTAMP,
    documentation_version           VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
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

CREATE TABLE referencedatamodel.data_element (
    id                          UUID         NOT NULL,
    version                     INT8         NOT NULL,
    date_created                TIMESTAMP    NOT NULL,
    reference_data_model_id     UUID         NOT NULL,
    last_updated                TIMESTAMP    NOT NULL,
    path                        TEXT         NOT NULL,
    depth                       INT4         NOT NULL,
    min_multiplicity            INT4,
    max_multiplicity            INT4,
    breadcrumb_tree_id          UUID         NOT NULL,
    data_type_id                UUID         NOT NULL,
    idx                         INT4         NOT NULL,
    created_by                  VARCHAR(255) NOT NULL,
    aliases_string              TEXT,
    label                       TEXT         NOT NULL,
    description                 TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE referencedatamodel.data_type (
    id                          UUID         NOT NULL,
    version                     INT8         NOT NULL,
    date_created                TIMESTAMP    NOT NULL,
    domain_type                 VARCHAR(15)  NOT NULL,
    last_updated                TIMESTAMP    NOT NULL,
    path                        TEXT         NOT NULL,
    depth                       INT4         NOT NULL,
    breadcrumb_tree_id          UUID         NOT NULL,
    reference_data_model_id     UUID         NOT NULL,
    idx                         INT4         NOT NULL,
    created_by                  VARCHAR(255) NOT NULL,
    aliases_string              TEXT,
    label                       TEXT         NOT NULL,
    description                 TEXT,
    class                       VARCHAR(255) NOT NULL,
    units                       VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX data_element_reference_data_model_idx ON referencedatamodel.data_element(reference_data_model_id);
CREATE INDEX data_element_data_type_idx ON referencedatamodel.data_element(data_type_id);
CREATE INDEX data_element_created_by_idx ON referencedatamodel.data_element(created_by);
CREATE INDEX reference_data_model_created_by_idx ON referencedatamodel.reference_data_model(created_by);
CREATE INDEX data_type_data_model_idx ON referencedatamodel.data_type(reference_data_model_id);
CREATE INDEX data_type_created_by_idx ON referencedatamodel.data_type(created_by);

ALTER TABLE IF EXISTS referencedatamodel.data_element
    ADD CONSTRAINT referencedata_data_element_data_model_fk FOREIGN KEY (reference_data_model_id) REFERENCES referencedatamodel.reference_data_model;
ALTER TABLE IF EXISTS referencedatamodel.data_element
    ADD CONSTRAINT referencedata_data_element_breadcrumb_tree_fk FOREIGN KEY (breadcrumb_tree_id) REFERENCES referencedatamodel.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedatamodel.data_element
    ADD CONSTRAINT referencedata_data_element_data_type_fk FOREIGN KEY (data_type_id) REFERENCES referencedatamodel.data_type;
