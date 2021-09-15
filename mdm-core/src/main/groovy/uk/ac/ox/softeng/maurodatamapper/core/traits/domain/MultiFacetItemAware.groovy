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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.compiler.GrailsCompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @since 30/01/2020
 */
@GrailsCompileStatic
trait MultiFacetItemAware extends MdmDomain {

    private static final Logger log = LoggerFactory.getLogger(getClass())

    UUID multiFacetAwareItemId
    String multiFacetAwareItemDomainType
    MultiFacetAware multiFacetAwareItem

    abstract String getEditLabel()

    //static transients = ['multiFacetAwareItem']

    void setMultiFacetAwareItem(MultiFacetAware multiFacetAwareItem) {
        this.multiFacetAwareItem = multiFacetAwareItem
        multiFacetAwareItemId = multiFacetAwareItem?.id
        multiFacetAwareItemDomainType = multiFacetAwareItem?.domainType
    }

    /**
     * Get the path of the facet including the MFA path it resides inside.
     * If the multiFacetAwareItem field is set then there is no need to pass in the parameter.
     *
     * @param multiFacetAwareItemIfUnset The MFA to path from incase the facet only has the MFA id and MFA domaintype
     * @return
     */
    Path getFullPathInsideMultiFacetAwareItem(MultiFacetAware multiFacetAwareItemIfUnset = multiFacetAwareItem) {
        if (!multiFacetAwareItemIfUnset) {
            log.error('The MultiFacetAware Item is not set inside the facet, it needs to be passed into the method')
            return path
        }
        if (multiFacetAwareItemIfUnset.id != multiFacetAwareItemId) {
            log.error('The facet does not reside inside the requested MultiFacetAware Item')
            return path
        }
        Path.from(multiFacetAwareItemIfUnset.path, path)
    }
}
