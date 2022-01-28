/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
 * @since 20/01/2022
 */
class Expectations {

    private UserExpectation anonymous = new UserExpectation('anonymous users')
    private UserExpectation authenticated = new UserExpectation('authenticated users')
    private UserExpectation reader = new UserExpectation('readers')
    private UserExpectation reviewer = new UserExpectation('reviewers')
    private UserExpectation author = new UserExpectation('authors')
    private UserExpectation editor = new UserExpectation('editors')
    private UserExpectation containerAdmin = new UserExpectation('container admins')

    private boolean isSoftDeleteByDefault
    private boolean accessPermissionIsInherited
    private boolean hasDefaultCreation
    private boolean mergingIsAvailable
    private boolean noAccess
    private boolean availableActionsProvided
    private boolean editorCanChangePermissions
    private boolean isSecuredResource

    private Expectations() {

    }

    boolean getAccessPermissionIsNotInherited() {
        !accessPermissionIsInherited
    }

    boolean getIsSoftDeleteByDefault() {
        isSoftDeleteByDefault
    }

    boolean getAccessPermissionIsInherited() {
        accessPermissionIsInherited
    }

    boolean getEditorCanChangePermissions() {
        editorCanChangePermissions
    }

    boolean getHasDefaultCreation() {
        hasDefaultCreation
    }

    boolean getMergingIsAvailable() {
        mergingIsAvailable
    }

    List<String> getContainerAdminAvailableActions() {
        containerAdmin.availableActions()
    }

    List<String> getEditorAvailableActions() {
        editor.availableActions()
    }

    List<String> getAuthorAvailableActions() {
        author.availableActions()
    }

    List<String> getReviewerAvailableActions() {
        reviewer.availableActions()
    }

    List<String> getReaderAvailableActions() {
        reader.availableActions()
    }

    boolean getNoAccess() {
        noAccess
    }

    boolean getAvailableActionsProvided() {
        availableActionsProvided
    }

    boolean getIsSecuredResource() {
        isSecuredResource
    }

    boolean containerAdminsCan(String action) {
        containerAdmin.can(action)
    }

    boolean editorsCan(String action) {
        editor.can(action)
    }

    boolean authorsCan(String action) {
        author.can(action)
    }

    boolean reviewersCan(String action) {
        reviewer.can(action)
    }

    boolean readersCan(String action) {
        reader.can(action)
    }

    boolean authenticatedUsersCan(String action) {
        authenticated.can(action)
    }

    boolean anonymousUsersCan(String action) {
        anonymous.can(action)
    }

    boolean can(String name, String action) {
        switch (name) {
            case 'Authenticated': return authenticatedUsersCan(action)
            case 'Reader': return readersCan(action)
            case 'Reviewer': return reviewersCan(action)
            case 'Author': return authorsCan(action)
            case 'Editor': return editorsCan(action)
            case 'ContainerAdmin': return containerAdminsCan(action)
            case 'Admin': return true
        }
        anonymousUsersCan(action)
    }

    List<String> availableActions(String name) {
        switch (name) {
            case 'Authenticated': return authenticated.availableActions()
            case 'Reader': return reader.availableActions()
            case 'Reviewer': return reviewer.availableActions()
            case 'Author': return author.availableActions()
            case 'Editor': return editor.availableActions()
            case 'ContainerAdmin': return containerAdmin.availableActions()
            case 'Admin': return containerAdmin.availableActions()
        }
        anonymous.availableActions()
    }

    /**
     * DSL
     */

    Expectations withSoftDeleteByDefault() {
        isSoftDeleteByDefault = true
        this
    }

    Expectations withoutSoftDeleteByDefault() {
        isSoftDeleteByDefault = false
        this
    }

    Expectations whereTestingSecuredResource() {
        isSecuredResource = true
        this
    }

    Expectations whereTestingUnsecuredResource() {
        isSecuredResource = false
        this
    }

    Expectations withDefaultCreation() {
        this.hasDefaultCreation = true
        this
    }

    Expectations withoutDefaultCreation() {
        this.hasDefaultCreation = false
        this
    }

    Expectations withInheritedAccessPermissions() {
        this.accessPermissionIsInherited = true
        this
    }

    Expectations withoutInheritedAccessPermissions() {
        this.accessPermissionIsInherited = false
        this
    }

    Expectations withAvailableActions() {
        this.availableActionsProvided = true
        this
    }

    Expectations withoutAvailableActions() {
        this.availableActionsProvided = false
        this
    }

    Expectations withMergingAvailable() {
        mergingIsAvailable = true
        this
    }

    Expectations withoutMergingAvailable() {
        mergingIsAvailable = false
        this
    }

    Expectations whereEditorsCanChangePermissions() {
        this.editorCanChangePermissions = true
        this
    }

    Expectations whereEditorsCannotChangePermissions() {
        this.editorCanChangePermissions = false
        this
    }

    Expectations whereContainerAdmins(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(containerAdmin)
        closure.call()
        this
    }

    Expectations whereEditors(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(editor)
        closure.call()
        this
    }

    Expectations whereAuthors(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(author)
        closure.call()
        this
    }

    Expectations whereReviewers(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(reviewer)
        closure.call()
        this
    }

    Expectations whereReaders(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(reader)
        closure.call()
        this
    }

    Expectations whereAuthenticatedUsers(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(authenticated)
        closure.call()
        this
    }

    Expectations whereAnonymousUsers(@DelegatesTo(UserExpectation) Closure closure) {
        closure.setDelegate(anonymous)
        closure.call()
        this
    }

    Expectations whereReadersCanAction(String... actions) {
        this.reader.canAction actions
        this
    }

    Expectations whereReviewersCanAction(String... actions) {
        this.reviewer.canAction actions
        this
    }

    Expectations whereAuthorsCanAction(String... actions) {
        this.author.canAction actions
        this
    }

    Expectations whereEditorsCanAction(String... actions) {
        this.editor.canAction actions
        this
    }

    Expectations whereContainerAdminsCanAction(String... actions) {
        this.containerAdmin.canAction actions
        this
    }

    Expectations whereNooneCanDoAnything() {
        withoutSoftDeleteByDefault()
        withoutDefaultCreation()
        withInheritedAccessPermissions()
        withoutMergingAvailable()
        containerAdmin.hasNoAccess()
        editor.hasNoAccess()
        author.hasNoAccess()
        reviewer.hasNoAccess()
        reader.hasNoAccess()
        authenticated.hasNoAccess()
        anonymous.hasNoAccess()
        noAccess = true
        this
    }

    Expectations withDefaultExpectations() {
        withoutSoftDeleteByDefault()
        withoutDefaultCreation()
        withoutInheritedAccessPermissions()
        withAvailableActions()
        withMergingAvailable()
        whereTestingSecuredResource()
        whereEditorsCanChangePermissions()
        containerAdmin.canUpdate().withDefaultActions()
        editor.canUpdate().withDefaultActions()
        author.canSee().canEditDescription().withDefaultActions()
        reviewer.canSee().withDefaultActions()
        reader.canSee().withDefaultActions()
        authenticated.hasNoAccess()
        anonymous.hasNoAccess()
        this
    }

    static Expectations builder() {
        new Expectations()
    }


    String toString() {
        StringBuilder sb = new StringBuilder('Expectations are ')

        if (isSoftDeleteByDefault) sb.append('\n  with soft deletion by default')
        else sb.append('\n  without soft deletion by default')

        if (hasDefaultCreation) sb.append('\n  with default creation possible')
        else sb.append('\n  without default creation possible')

        if (accessPermissionIsInherited) sb.append('\n  with access permissions inherited')
        else sb.append('\n  without access permissions inherited')

        if (isSecuredResource) sb.append('\n  with testing secured resource')
        else sb.append('\n  with testing unsecured resource')

        sb.append('\n  where ').append(anonymous)
        sb.append('\n  where ').append(authenticated)
        sb.append('\n  where ').append(reader)
        sb.append('\n  where ').append(reviewer)
        sb.append('\n  where ').append(author)
        sb.append('\n  where ').append(editor)
        sb.append('\n  where ').append(containerAdmin)

        if (availableActionsProvided) {

            sb.append('\n  with container admin available actions: ').append(containerAdminAvailableActions)
            sb.append('\n  with editor available actions: ').append(editorAvailableActions)
            sb.append('\n  with author available actions: ').append(authorAvailableActions)
            sb.append('\n  with reviewer available actions: ').append(reviewerAvailableActions)
            sb.append('\n  with reader available actions: ').append(readerAvailableActions)
        } else {
            sb.append('\n  with no available actions provided')
        }


        sb.toString()
    }


}
