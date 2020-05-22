/*
 * Copyright 2020 University of Oxford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search

import grails.validation.Validateable

class SearchParams implements Validateable {

    String domainType
    Integer max
    Integer offset
    String searchTerm
    Boolean labelOnly = false
    List<String> domainTypes = []
    List<String> dataModelTypes = []
    Date lastUpdatedBefore
    Date lastUpdatedAfter
    Date createdBefore
    Date createdAfter
    List<String> classifiers = []
    List<List> classifierFilter = []
    String sortField = ""

    static constraints = {
        domainType nullable: true
        offset nullable: true, min: 0
        max nullable: true, min: 0
        searchTerm nullable: true, blank: false
        lastUpdatedBefore nullable: true
        lastUpdatedAfter nullable: true
        createdBefore nullable: true
        createdAfter nullable: true
    }

    void setLimit(Integer limit) {
        max = limit
    }

    void setSearch(String term) {
        searchTerm = term
    }

    List<String> getDomainTypes() {
        domainTypes ?: domainType ? [domainType] : []
    }
}
