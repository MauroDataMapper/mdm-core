/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.CatalogueFileConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFile
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceFile implements CatalogueFile, MultiFacetItemAware {

    UUID id

    static constraints = {
        CallableConstraints.call(CatalogueFileConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.multiFacetAwareItem && !obj.multiFacetAwareItem.ident()) return true
            ['default.null.message']
        }
    }

    static mapping = {
    }

    static transients = ['multiFacetAwareItem']

    ReferenceFile() {
    }

    @Override
    String getDomainType() {
        ReferenceFile.simpleName
    }

    @Override
    String getPathPrefix() {
        'rf'
    }

    def beforeValidate() {
        fileSize = fileContents?.size()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${fileName}"
    }

    static DetachedCriteria<ReferenceFile> by() {
        new DetachedCriteria<ReferenceFile>(ReferenceFile)
    }

    static DetachedCriteria<ReferenceFile> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        new DetachedCriteria<ReferenceFile>(ReferenceFile).eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<ReferenceFile> byMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        new DetachedCriteria<ReferenceFile>(ReferenceFile).inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<ReferenceFile> byMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceFile> withFilter(DetachedCriteria<ReferenceFile> criteria, Map filters) {
        withBaseFilter(criteria, filters)
    }
}