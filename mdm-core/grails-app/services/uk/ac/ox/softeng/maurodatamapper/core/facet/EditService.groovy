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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.security.UserService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class EditService implements AnonymisableService {

    @Autowired(required = false)
    UserService userService

    MessageSource messageSource
    @Autowired(required = false)
    List<ModelService> modelServices

    Edit get(Serializable id) {
        Edit.get(Utils.toUuid(id))
    }

    List<Edit> list(Map pagination) {
        Edit.list(pagination)
    }

    Long count() {
        Edit.count()
    }

    Edit save(Edit edit) {
        edit.save(flush: true)
    }

    List<Edit> findAllByResource(String resourceDomainType, UUID resourceId, Map pagination = [sort: 'dateCreated', order: 'asc']) {
        Edit.findAllByResource(resourceDomainType, resourceId, pagination)
    }

    List<Edit> findAllByResourceAndTitle(String resourceDomainType, UUID resourceId, EditTitle title, Map pagination = [sort: 'dateCreated', order: 'asc']) {
        Edit.findAllByResourceAndTitle(resourceDomainType, resourceId, title, pagination)
    }

    void createAndSaveEdit(EditTitle title, UUID resourceId, String resourceDomainType, String description, User createdBy, boolean flush = false) {
        Edit edit = new Edit(title: title, resourceId: resourceId, resourceDomainType: resourceDomainType,
                             description: description, createdBy: createdBy.emailAddress)
        if (edit.validate()) {
            edit.save(flush: flush, validate: false)
        } else {
            throw new ApiInvalidModelException('ES01', 'Created Edit is invalid', edit.errors, messageSource)
        }
    }

    Edit populateEditUser(Edit edit) {
        if (!edit) return null
        edit.user = userService ? userService.findUser(edit.createdBy) ?: AnonymousUser.instance : AnonymousUser.instance
        edit
    }

    void anonymise(String createdBy) {
        Edit.findAllByCreatedBy(createdBy).each {edit ->
            edit.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS
            edit.save(validate: false)
        }
    }
}