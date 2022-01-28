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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class VersionLinkInterceptor extends FacetInterceptor {

    @Override
    Class getFacetClass() {
        VersionLink
    }

    @Override
    String getOwningType() {
        'model'
    }

    boolean before() {
        facetResourceChecks()

        if (!Utils.parentClassIsAssignableFromChild(VersionLinkAware, getOwningClass())) {
            throw new ApiBadRequestException('VLI01', "Domain class [${params.modelDomainType}] does not extend the VersionLinkAware trait")
        }

        checkActionAllowedOnFacet()
    }
}