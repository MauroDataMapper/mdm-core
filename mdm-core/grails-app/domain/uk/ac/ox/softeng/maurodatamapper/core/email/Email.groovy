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
package uk.ac.ox.softeng.maurodatamapper.core.email

import grails.gorm.DetachedCriteria

import java.time.OffsetDateTime

class Email {

    UUID id
    String sentToEmailAddress
    String subject
    String body
    String emailServiceUsed
    OffsetDateTime dateTimeSent
    Boolean successfullySent
    String failureReason

    static constraints = {
        emailServiceUsed nullable: true
        failureReason nullable: true
        dateTimeSent nullable: true
    }

    static mapping = {
        body type: 'text'
        subject type: 'text'
        failureReason type: 'text'
    }

    static DetachedCriteria<Email> by() {
        new DetachedCriteria<Email>(Email)
    }

    static DetachedCriteria<Email> withFilter(Map filters) {
        DetachedCriteria<Email> criteria = by()
        if (filters.sentToEmailAddress) criteria = criteria.ilike('sentToEmailAddress', "%${filters.sentToEmailAddress}%")
        if (filters.subject) criteria = criteria.ilike('subject', "%${filters.subject}%")
        if (filters.emailServiceUsed) criteria = criteria.ilike('emailServiceUsed', "%${filters.emailServiceUsed}%")
        if (filters.successfullySent) criteria = criteria.eq('successfullySent', filters.successfullySent)
        criteria
    }
}
