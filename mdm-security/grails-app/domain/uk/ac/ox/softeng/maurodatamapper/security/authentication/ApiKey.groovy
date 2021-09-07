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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.rest.Resource

import java.time.LocalDate

@Resource(readOnly = false, formats = ['json', 'xml'])
class ApiKey implements MdmDomain {

    public static final String DEFAULT_NAME = 'default'

    UUID id
    String name
    LocalDate expiryDate
    Boolean refreshable
    Boolean disabled

    static belongsTo = [catalogueUser: CatalogueUser]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        name blank: false, unique: 'catalogueUser'
    }

    static mapping = {
        catalogueUser fetch: 'join'
    }

    @Override
    String getDomainType() {
        ApiKey.simpleName
    }

    ApiKey() {
        refreshable = false
        name = DEFAULT_NAME
        disabled = false
    }

    boolean isExpired() {
        expiryDate.isBefore(LocalDate.now())
    }

    void setExpiresInDays(long days) {
        expiryDate = LocalDate.now().plusDays(days)
    }
}