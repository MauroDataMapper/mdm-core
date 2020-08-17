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

CREATE TABLE core.authority (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    url                             VARCHAR(255) NOT NULL,
    created_by                      VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX authority_created_by_idx ON core.authority(created_by);
ALTER TABLE IF EXISTS core.authority
    ADD CONSTRAINT UKfcae2aea4497b223b1762d7b79a3 UNIQUE (url, label);
