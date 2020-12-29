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

CREATE TABLE core.model_extend (
    id                                  UUID         NOT NULL,
    version                             INT8         NOT NULL,
    date_created                        TIMESTAMP    NOT NULL,
    last_updated                        TIMESTAMP    NOT NULL,    
    catalogue_item_id                   UUID,    
    catalogue_item_domain_type          VARCHAR(255) NOT NULL,    
    extended_catalogue_item_id          UUID         NOT NULL,
    extended_catalogue_item_domain_type VARCHAR(255) NOT NULL,
    created_by                          VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);


CREATE INDEX model_extend_extended_catalogue_item_idx ON core.model_extend(extended_catalogue_item_id);
CREATE INDEX model_extend_catalogue_item_idx ON core.model_extend(catalogue_item_id);
CREATE INDEX model_extend_created_by_idx ON core.model_extend(created_by);
