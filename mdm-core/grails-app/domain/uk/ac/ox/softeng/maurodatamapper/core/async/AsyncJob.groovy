/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria

import java.time.OffsetDateTime

/**
 * @since 14/03/2022
 */
class AsyncJob implements MdmDomain {

    UUID id
    String jobName
    String startedByUser
    OffsetDateTime dateTimeStarted
    String status
    String message

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        startedByUser email: true
        message nullable: true, blank: false
    }

    static mapping = {
        message type: 'text'
    }

    @Override
    String getDomainType() {
        AsyncJob.simpleName
    }

    @Override
    String getPathPrefix() {
        return null
    }

    @Override
    String getPathIdentifier() {
        return null
    }

    static DetachedCriteria<AsyncJob> by() {
        new DetachedCriteria<AsyncJob>(AsyncJob)
    }

    static DetachedCriteria<AsyncJob> byStartedByUser(String emailAddress) {
        by().eq('startedByUser', emailAddress)
    }

    static DetachedCriteria<AsyncJob> withFilter(DetachedCriteria<AsyncJob> criteria, Map filters) {
        if (filters.jobName) criteria = criteria.ilike('jobName', "%${filters.jobName}%")
        if (filters.startedByUser) criteria = criteria.ilike('startedByUser', "%${filters.startedByUser}%")
        if (filters.status) criteria = criteria.ilike('status', "%${filters.status}%")
        criteria
    }
}
