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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation

/**
 * @since 24/01/2022
 */
class UserExpectation {
    private String name
    private boolean canCreate
    private boolean canUpdate
    private boolean canEditDescription
    private boolean canDelete
    private boolean canSee
    private boolean canIndex
    private List<String> availableActions

    UserExpectation(String name) {
        this.name = name
    }

    UserExpectation hasNoAccess() {
        canIndex = false
        canCreate = false
        canUpdate = false
        canEditDescription = false
        canDelete = false
        canSee = false
        availableActions = []
        this
    }

    UserExpectation canUpdate() {
        this.canUpdate = true
        this
    }

    UserExpectation canDelete() {
        this.canDelete = true
        this
    }

    UserExpectation canCreate() {
        this.canCreate = true
        this
    }

    UserExpectation canSee() {
        this.canSee = true
        this
    }

    UserExpectation canEditDescription() {
        this.canEditDescription = true
        this
    }

    UserExpectation canSoftDelete() {
        this.canDelete = true
        this
    }

    UserExpectation canIndex() {
        this.canIndex = true
        this
    }

    UserExpectation cannotUpdate() {
        this.canUpdate = false
        this
    }

    UserExpectation cannotEditDescription() {
        this.canEditDescription = false
        this
    }

    UserExpectation cannotDelete() {
        this.canDelete = false
        this
    }

    UserExpectation cannotCreate() {
        this.canCreate = false
        this
    }

    UserExpectation cannotSee() {
        this.canSee = false
        this
    }

    UserExpectation cannotIndex() {
        this.canIndex = false
        this
    }

    UserExpectation canAction(String... actions) {
        this.availableActions = actions.toList()
        this
    }

    UserExpectation withDefaultActions() {
        if (canUpdate) {
            canAction 'delete', 'save', 'show', 'update'
        } else if (canCreate) {
            canAction 'delete', 'save', 'show'
        } else if (canSee) {
            canAction('show')
        }
        this
    }

    boolean can(String action) {
        switch (action) {
            case 'update': return canUpdate
            case 'delete': return canDelete
            case 'create': return canCreate
            case 'see': return canSee
            case 'editDescription': return canEditDescription
            case 'index': return canIndex
        }
        false
    }

    List<String> availableActions() {
        availableActions.sort()
    }

    String toString() {
        StringBuilder sb = new StringBuilder(name).append(' ')

        if (canUpdate) sb.append('can update, ')
        else sb.append('cannot update, ')

        if (canEditDescription) sb.append('can editDescription, ')
        else sb.append('cannot editDescription, ')

        if (canDelete) sb.append('can delete, ')
        else sb.append('cannot delete, ')

        if (canCreate) sb.append('can create, ')
        else sb.append('cannot create, ')

        if (canSee) sb.append('can see, ')
        else sb.append('cannot see, ')

        if (canIndex) sb.append('can index')
        else sb.append('cannot index')

        sb.toString()
    }
}
