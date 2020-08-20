/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional

@Transactional
class AuthorityService {
    GrailsApplication grailsApplication

    Authority get(Serializable id) {
        Authority.get(id)
    }

    List<Authority> list(Map pagination) {
        pagination ? Authority.list(pagination) : Authority.list()
    }

    Long count() {
        Authority.count()
    }

    void delete(Authority authority) {
        authority.delete(flush: true)
    }

    Authority save(Authority authority) {
        authority.save(failOnError: true, validate: false)
    }

    Authority getDefaultAuthority() {
        Authority.findByLabel(grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY))
    }

    boolean defaultAuthorityExists() {
        Authority.countByLabel(grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY)) > 0
    }
}