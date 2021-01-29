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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 26/09/2017
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait EditHistoryAware extends AddsEditHistory implements CreatorAware {

    void addToEditsTransactionally(User createdBy, String description) {
        createAndSaveEditInsideNewTransaction createdBy, description
    }

    void addToEditsTransactionally(User createdBy, String editLabel, List<String> dirtyPropertyNames) {
        if (shouldAddEdit(dirtyPropertyNames)) {
            createAndSaveEditInsideNewTransaction createdBy, "[$editLabel] changed properties ${editedPropertyNames(dirtyPropertyNames)}"
        }
    }

    void createAndSaveEditInsideNewTransaction(User createdBy, String description) {
        Edit edit = null
        Edit.withNewTransaction {
            edit = new Edit(createdBy: createdBy.emailAddress,
                            description: description,
                            resourceId: id,
                            resourceDomainType: domainType).save(validate: false)
        }
        if (edit) {
            edit.skipValidation(true)
            skipValidation(true)
        }
    }

    @Override
    void addCreatedEdit(User creator) {
        addToEditsTransactionally creator, getCreatedEditDescription()
    }

    String getCreatedEditDescription() {
        "[$editLabel] created"
    }

    @Override
    void addUpdatedEdit(User editor, List<String> dirtyPropertyNames) {
        addToEditsTransactionally editor, editLabel, dirtyPropertyNames
    }

    @Override
    void addDeletedEdit(User deleter) {
        // No-op
    }

    List<Edit> getEdits() {
        Edit.findAllByResource(domainType, id)
    }

}
