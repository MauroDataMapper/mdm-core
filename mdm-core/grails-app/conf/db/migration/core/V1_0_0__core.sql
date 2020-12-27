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

CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE core.annotation (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    path                       TEXT         NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    depth                      INT4         NOT NULL,
    catalogue_item_id          UUID,
    parent_annotation_id       UUID,
    created_by                 VARCHAR(255) NOT NULL,
    label                      TEXT         NOT NULL,
    description                TEXT,
    child_annotations_idx      INT4,
    PRIMARY KEY (id)
);
CREATE TABLE core.api_property (
    id              UUID         NOT NULL,
    version         INT8         NOT NULL,
    last_updated_by VARCHAR(255) NOT NULL,
    date_created    TIMESTAMP    NOT NULL,
    last_updated    TIMESTAMP    NOT NULL,
    value           TEXT         NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    key             VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.breadcrumb_tree (
    id                  UUID         NOT NULL,
    version             INT8         NOT NULL,
    domain_type         VARCHAR(255) NOT NULL,
    finalised           BOOLEAN,
    domain_id           UUID,
    tree_string         TEXT         NOT NULL,
    top_breadcrumb_tree BOOLEAN      NOT NULL,
    label               TEXT,
    parent_id           UUID,
    PRIMARY KEY (id)
);
CREATE TABLE core.classifier (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    path                            TEXT         NOT NULL,
    depth                           INT4         NOT NULL,
    parent_classifier_id            UUID,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    created_by                      VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE core.edit (
    id                   UUID         NOT NULL,
    version              INT8         NOT NULL,
    date_created         TIMESTAMP    NOT NULL,
    last_updated         TIMESTAMP    NOT NULL,
    resource_domain_type VARCHAR(255) NOT NULL,
    resource_id          UUID         NOT NULL,
    created_by           VARCHAR(255) NOT NULL,
    description          VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.email (
    id                    UUID         NOT NULL,
    version               INT8         NOT NULL,
    sent_to_email_address VARCHAR(255) NOT NULL,
    successfully_sent     BOOLEAN      NOT NULL,
    body                  TEXT         NOT NULL,
    date_time_sent        TIMESTAMP    NOT NULL,
    email_service_used    VARCHAR(255) NOT NULL,
    failure_reason        TEXT,
    subject               TEXT         NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.folder (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    path                            TEXT         NOT NULL,
    deleted                         BOOLEAN      NOT NULL,
    depth                           INT4         NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    parent_folder_id                UUID,
    created_by                      VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE core.metadata (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    namespace                  TEXT         NOT NULL,
    catalogue_item_id          UUID,
    value                      TEXT         NOT NULL,
    created_by                 VARCHAR(255) NOT NULL,
    key                        TEXT         NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.reference_file (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    file_size                  INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    file_type                  VARCHAR(255) NOT NULL,
    file_name                  VARCHAR(255) NOT NULL,
    file_contents              BYTEA        NOT NULL,
    catalogue_item_id          UUID,
    created_by                 VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.semantic_link (
    id                                UUID         NOT NULL,
    version                           INT8         NOT NULL,
    date_created                      TIMESTAMP    NOT NULL,
    target_catalogue_item_id          UUID         NOT NULL,
    last_updated                      TIMESTAMP    NOT NULL,
    catalogue_item_domain_type        VARCHAR(255) NOT NULL,
    target_catalogue_item_domain_type VARCHAR(255) NOT NULL,
    link_type                         VARCHAR(255) NOT NULL,
    catalogue_item_id                 UUID,
    created_by                        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.user_image_file (
    id            UUID         NOT NULL,
    version       INT8         NOT NULL,
    file_size     INT8         NOT NULL,
    date_created  TIMESTAMP    NOT NULL,
    last_updated  TIMESTAMP    NOT NULL,
    file_type     VARCHAR(255) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    user_id       UUID         NOT NULL,
    file_contents BYTEA        NOT NULL,
    created_by    VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE core.version_link (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    target_model_domain_type   VARCHAR(255) NOT NULL,
    link_type                  VARCHAR(255) NOT NULL,
    target_model_id            UUID         NOT NULL,
    catalogue_item_id          UUID,
    created_by                 VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX annotation_parent_annotation_idx ON core.annotation(parent_annotation_id);
CREATE INDEX annotation_created_by_idx ON core.annotation(created_by);
CREATE INDEX apiProperty_created_by_idx ON core.api_property(created_by);
CREATE INDEX classifier_parent_classifier_idx ON core.classifier(parent_classifier_id);
CREATE INDEX classifier_created_by_idx ON core.classifier(created_by);
ALTER TABLE IF EXISTS core.classifier
    ADD CONSTRAINT UK_j7bbt97ko557eewc3u50ha8ko UNIQUE (label);
CREATE INDEX edit_created_by_idx ON core.edit(created_by);
CREATE INDEX folder_parent_folder_idx ON core.folder(parent_folder_id);
CREATE INDEX folder_created_by_idx ON core.folder(created_by);
CREATE INDEX metadata_catalogue_item_idx ON core.metadata(catalogue_item_id);
CREATE INDEX metadata_created_by_idx ON core.metadata(created_by);
CREATE INDEX referenceFile_created_by_idx ON core.reference_file(created_by);
CREATE INDEX semantic_link_target_catalogue_item_idx ON core.semantic_link(target_catalogue_item_id);
CREATE INDEX semantic_link_catalogue_item_idx ON core.semantic_link(catalogue_item_id);
CREATE INDEX semanticLink_created_by_idx ON core.semantic_link(created_by);
CREATE INDEX userImageFile_created_by_idx ON core.user_image_file(created_by);
CREATE INDEX version_link_target_model_idx ON core.version_link(target_model_id);
CREATE INDEX version_link_catalogue_item_idx ON core.version_link(catalogue_item_id);
CREATE INDEX versionLink_created_by_idx ON core.version_link(created_by);
ALTER TABLE IF EXISTS core.annotation
    ADD CONSTRAINT FKnrnwt8d2s4kytg7mis2rg2a5x FOREIGN KEY (parent_annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS core.breadcrumb_tree
    ADD CONSTRAINT FK1hraqwgiiva4reb2v6do4it81 FOREIGN KEY (parent_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS core.classifier
    ADD CONSTRAINT FKahkm58kcer6a9q2v01ealovr6 FOREIGN KEY (parent_classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS core.folder
    ADD CONSTRAINT FK57g7veis1gp5wn3g0mp0x57pl FOREIGN KEY (parent_folder_id) REFERENCES core.folder;