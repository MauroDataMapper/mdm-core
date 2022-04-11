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


import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

class Edit implements MdmDomain {

    UUID id
    EditTitle title
    String description
    String resourceDomainType
    UUID resourceId
    User user

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        resourceDomainType nullable: false, blank: false
        resourceId nullable: false
        title nullable: false
    }

    static mapping = {
        batchSize(10)
        description type: 'text'
    }

    static transients = ['user']

    Edit() {
    }

    @Override
    String getDomainType() {
        Edit.simpleName
    }

    @Override
    String getPathPrefix() {
        'ed'
    }

    @Override
    String getPathIdentifier() {
        title
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static List<Edit> findAllByResource(String resourceDomainType, UUID resourceId, Map pagination = [:]) {
        Edit.findAllByResourceDomainTypeAndResourceId(resourceDomainType, resourceId, pagination)
    }

    static List<Edit> findAllByResourceAndTitle(String resourceDomainType, UUID resourceId, EditTitle title, Map pagination = [:]) {
        Edit.findAllByResourceDomainTypeAndResourceIdAndTitle(resourceDomainType, resourceId, title, pagination)
    }
}