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

CREATE TABLE core.subscribed_catalogue (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL, 
    created_by                      VARCHAR(255) NOT NULL,
    url                             VARCHAR(255) NOT NULL,
    api_key                         UUID         NOT NULL,
    refresh_period                  INT          NOT NULL,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    last_read                       TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE core.subscribed_model (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL, 
    created_by                      VARCHAR(255) NOT NULL,
    subscribed_catalogue_id         UUID         NOT NULL,
    subscribed_model_id             UUID         NOT NULL,
    subscribed_model_type           VARCHAR(255) NOT NULL,
    folder_id                       UUID         NOT NULL,
    last_read                       TIMESTAMP,
    local_model_id                  UUID,
    PRIMARY KEY (id)
);

CREATE INDEX subscribed_model_subscribed_catalogue_id ON core.subscribed_model(subscribed_catalogue_id);
CREATE INDEX subscribed_model_local_model_id ON core.subscribed_model(local_model_id);
