/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType

/**
 * @since 26/09/2017
 */
@SelfType(MdmDomain)
@GrailsCompileStatic
trait EditHistoryAware {

    public static final List<String> DIRTY_PROPERTY_NAMES_TO_IGNORE = ['version', 'lastUpdated']

    abstract String getEditLabel()

    boolean shouldAddEdit(List<String> dirtyPropertyNames) {
        editedPropertyNames(dirtyPropertyNames)
    }

    @SuppressFBWarnings('BC_IMPOSSIBLE_INSTANCEOF')
    List<String> editedPropertyNames(List<String> dirtyPropertyNames) {
        dirtyPropertyNames - DIRTY_PROPERTY_NAMES_TO_IGNORE
    }

    void addToEditsTransactionally(EditTitle title, User createdBy, String description) {
        createAndSaveEditInsideNewTransaction title, createdBy, description
    }

    void addToEditsTransactionally(EditTitle title, User createdBy, String editLabel, List<String> dirtyPropertyNames) {
        if (shouldAddEdit(dirtyPropertyNames)) {
            createAndSaveEditInsideNewTransaction title, createdBy, "[$editLabel] changed properties ${editedPropertyNames(dirtyPropertyNames)}"
        }
    }

    void createAndSaveEditInsideNewTransaction(EditTitle title, User createdBy, String description) {
        Edit edit = null
        Edit.withNewTransaction {
            edit = new Edit(title: title,
                            createdBy: createdBy.emailAddress,
                            description: description,
                            resourceId: id,
                            resourceDomainType: domainType).save(validate: false)
        }
        if (edit) {
            edit.skipValidation(true)
            skipValidation(true)
        }
    }

    void addCreatedEdit(User creator) {
        addToEditsTransactionally EditTitle.CREATE, creator, getCreatedEditDescription()
    }

    String getCreatedEditDescription() {
        "[$editLabel] created"
    }

    void addUpdatedEdit(User editor, List<String> dirtyPropertyNames) {
        addToEditsTransactionally EditTitle.UPDATE, editor, editLabel, dirtyPropertyNames
    }

    @SuppressWarnings('UnusedMethodParameter')
    void addDeletedEdit(User deleter) {
        // No-op
    }

    void addChangeNoticeEdit(User changer, String changeNotice) {
        addToEditsTransactionally EditTitle.CHANGENOTICE, changer, changeNotice
    }

    List<Edit> getEdits() {
        Edit.findAllByResource(domainType, id)
    }

}
