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
package uk.ac.ox.softeng.maurodatamapper.security


import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileService
import uk.ac.ox.softeng.maurodatamapper.security.rest.transport.UserProfilePicture
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils

import grails.gorm.transactions.Transactional

import java.security.NoSuchAlgorithmException
import java.time.OffsetDateTime

@Transactional
class AuthorityService {

    CatalogueUser get(Serializable id) {
        Authority.get(id)
    }

    List<CatalogueUser> list(Map pagination) {
        pagination ? Authority.withFilter(pagination).join('groups').list(pagination) : Authority.list()
    }

    Long count() {
        Authority.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(Authority authority) {
        Authority.disabled = true
    }

    List<CatalogueUser> findAllByAuthority(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(CatalogueUser)
        ids ? CatalogueUser.withFilter(pagination, CatalogueUser.byIdInList(ids)).list(pagination) : []
    }

}