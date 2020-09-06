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
package uk.ac.ox.softeng.maurodatamapper.security.policy

class ResourceActions {

    public static final String SHOW_ACTION = 'show'
    public static final String UPDATE_ACTION = 'update'
    public static final String DELETE_ACTION = 'delete'
    public static final String DISABLE_ACTION = 'disable'
    public static final String INDEX_ACTION = 'index'
    public static final String SAVE_ACTION = 'save'
    public static final String COMMENT_ACTION = 'comment'
    public static final String EDIT_DESCRIPTION_ACTION = 'editDescription'
    public static final String SOFT_DELETE_ACTION = 'softDelete'
    public static final String NEW_DOCUMENTATION_ACTION = 'newDocumentationVersion'
    public static final String NEW_MODEL_VERSION_ACTION = 'newBranchModelVersion'
    public static final String CAN_BE_FINALISED_ACTION = 'finalise'

    public static final List<String> READ_ONLY_ACTIONS = [SHOW_ACTION]
    public static final List<String> STANDARD_EDIT_ACTIONS = [SHOW_ACTION, UPDATE_ACTION, DELETE_ACTION]
    public static final List<String> STANDARD_CREATE_AND_EDIT_ACTIONS = [SHOW_ACTION, UPDATE_ACTION, DELETE_ACTION, SAVE_ACTION]
    public static final List<String> FULL_DELETE_ACTIONS = [SOFT_DELETE_ACTION, DELETE_ACTION]

    public static final List<String> MODEL_REVIEWER_ACTIONS = READ_ONLY_ACTIONS + [COMMENT_ACTION]
    public static final List<String> MODEL_AUTHOR_ACTIONS = MODEL_REVIEWER_ACTIONS + [EDIT_DESCRIPTION_ACTION]
    public static final List<String> MODEL_EDITOR_ACTIONS = MODEL_AUTHOR_ACTIONS + [UPDATE_ACTION, SAVE_ACTION, SOFT_DELETE_ACTION, CAN_BE_FINALISED_ACTION]
    public static final List<String> MODEL_CONTAINER_ADMIN_ACTIONS = MODEL_EDITOR_ACTIONS + [DELETE_ACTION]

    public static final List<String> CONTAINER_EDITOR_ACTIONS = READ_ONLY_ACTIONS + [UPDATE_ACTION, SAVE_ACTION, SOFT_DELETE_ACTION]
    public static final List<String> CONTAINER_CONTAINER_ADMIN_ACTIONS = CONTAINER_EDITOR_ACTIONS + [DELETE_ACTION]

    public static final List<String> MODEL_DISALLOWED_FINALISED_ACTIONS = [UPDATE_ACTION, SAVE_ACTION, EDIT_DESCRIPTION_ACTION, CAN_BE_FINALISED_ACTION]
    public static final List<String> MODELITEM_DISALLOWED_ACTIONS = [SOFT_DELETE_ACTION]

    public static final List<String> USER_ADMIN_ACTIONS = [SHOW_ACTION, UPDATE_ACTION, DISABLE_ACTION]
}
