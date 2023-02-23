/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService

import grails.gorm.transactions.Transactional

@Transactional
class AuthorityService implements SecurableResourceService<Authority>, MdmDomainService<Authority> {

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

    @Override
    Authority findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        String[] pids = pathIdentifier.split('@')
        Authority.findByLabelAndUrl(pids[0], pids[1])
    }

    @Override
    List<Authority> getAll(Collection<UUID> authorityIds) {
        Authority.getAll(authorityIds)
    }

    @Override
    List<Authority> list() {
        Authority.list()
    }

    @Override
    List<Authority> findAllReadableByEveryone() {
        Authority.findAllByReadableByEveryone(true)
    }

    @Override
    List<Authority> findAllReadableByAuthenticatedUsers() {
        Authority.findAllByReadableByAuthenticatedUsers(true)
    }

    Authority getDefaultAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    boolean defaultAuthorityExists() {
        Authority.countByDefaultAuthority(true) > 0
    }

    Authority findByLabel(String label) {
        Authority.findByLabel(label)
    }
}