/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(CatalogueItem)
@CompileStatic
trait ReferenceSummaryMetadataAware {

    abstract Set<ReferenceSummaryMetadata> getReferenceSummaryMetadata()

    def addToReferenceSummaryMetadata(ReferenceSummaryMetadata referenceSummaryMetadata) {
        referenceSummaryMetadata.setMultiFacetAwareItem(this as MultiFacetAware)
        addTo('referenceSummaryMetadata', referenceSummaryMetadata)
    }

    def addToReferenceSummaryMetadata(Map args) {
        addToReferenceSummaryMetadata(new ReferenceSummaryMetadata(args))
    }

    def removeFromReferenceSummaryMetadata(ReferenceSummaryMetadata referenceSummaryMetadata) {
        throw new ApiInternalException('FR01', 'Do not use removeFrom to remove facet from domain')
    }
}