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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria
import org.apache.commons.validator.routines.UrlValidator

import java.time.OffsetDateTime

class SubscribedCatalogue implements MdmDomain, SecurableResource, EditHistoryAware, InformationAware {

    private static final int DEFAULT_REFRESH_PERIOD = 7
    private static final String DEFAULT_CONNECTION_TIMEOUT_CONFIG_PROPERTY = 'maurodatamapper.federation.subscribedcatalogue.default.timeout'

    UUID id
    String url
    UUID apiKey
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers

    //Refresh period is assumed to be in units of days
    Integer refreshPeriod

    //The last time that we checked the catalogue for models to export
    OffsetDateTime lastRead

    //HTTP read connection timeout in minutes
    Integer connectionTimeout

    static hasMany = [
        subscribedModels: SubscribedModel
    ]

    static constraints = {
        CallableConstraints.call(InformationAwareConstraints, delegate)
        url blank: false, validator: { val ->
            new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(val) ?: ['default.invalid.url.message']
        }
        label unique: true
        refreshPeriod nullable: true
        lastRead nullable: true
        apiKey nullable: true
        connectionTimeout nullable: true
    }

    static mapping = {
        subscribedModels cascade: 'all-delete-orphan'
    }

    SubscribedCatalogue() {
        readableByAuthenticatedUsers = false
        readableByEveryone = false
    }

    void setUrl(String url) {
        if (url.endsWith("/")) {
            this.url = url.substring(0, url.lastIndexOf("/"));
        } else {
            this.url = url;
        }
    }

    @Override
    String getDomainType() {
        SubscribedCatalogue.simpleName
    }

    @Override
    String getPathPrefix() {
        null
    }

    @Override
    String getPathIdentifier() {
        null
    }

    @Override
    String getEditLabel() {
        "SubscribedCatalogue:${url}"
    }

    static DetachedCriteria<SubscribedCatalogue> by() {
        new DetachedCriteria<SubscribedCatalogue>(SubscribedCatalogue)
    }
}
