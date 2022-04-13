/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import grails.core.GrailsApplication
import grails.databinding.DataBindingSource
import grails.validation.Validateable
import grails.web.databinding.DataBindingUtils

class SearchParams implements Validateable {

    Integer max
    Integer offset
    // asc or desc
    String order
    // field to sort by
    String sort
    String searchTerm
    Boolean labelOnly
    List<String> domainTypes
    Date lastUpdatedBefore
    Date lastUpdatedAfter
    Date createdBefore
    Date createdAfter
    List<String> classifiers
    List<List> classifierFilter
    Map<String, Object> additionalParams = [:]

    static constraints = {
        offset nullable: true, min: 0
        max nullable: true, min: 0
        searchTerm nullable: true, blank: false
        lastUpdatedBefore nullable: true
        lastUpdatedAfter nullable: true
        createdBefore nullable: true
        createdAfter nullable: true
        sort nullable: true
        order nullable: true
    }

    private SearchParams() {
        domainTypes = []
        classifiers = []
        classifierFilter = []
        labelOnly = false
    }

    void setLimit(Integer limit) {
        max = limit
    }

    void setSearch(String term) {
        searchTerm = term
    }

    void setDomainType(String domainType) {
        domainTypes = [domainType]
    }

    def propertyMissing(String name) {
        additionalParams[name]
    }

    def propertyMissing(String name, def arg) {
        additionalParams[name] = arg
    }

    boolean containsKey(String key) {
        hasProperty(key) ?: additionalParams.containsKey(key)
    }

    Object getValue(String key) {
        additionalParams[key]
    }

    static SearchParams bind(GrailsApplication grailsApplication, def objectToBind) {

        DataBindingSource bindingSource = DataBindingUtils.createDataBindingSource(grailsApplication, SearchParams, objectToBind)
        SearchParams searchParams = new SearchParams()
        bindingSource.propertyNames.each {propName ->
            searchParams.setProperty(propName, bindingSource.getPropertyValue(propName))
        }
        searchParams.validate()
        searchParams
    }
}
