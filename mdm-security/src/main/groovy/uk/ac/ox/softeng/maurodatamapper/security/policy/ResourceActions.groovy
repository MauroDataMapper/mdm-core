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
package uk.ac.ox.softeng.maurodatamapper.security.policy

class ResourceActions {

    public static final String SHOW_ACTION = 'show'
    public static final String UPDATE_ACTION = 'update'
    public static final String CHANGE_FOLDER_ACTION = 'changeFolder'
    public static final String DELETE_ACTION = 'delete'
    public static final String DISABLE_ACTION = 'disable'
    public static final String INDEX_ACTION = 'index'
    public static final String SAVE_ACTION = 'save'
    public static final String COMMENT_ACTION = 'comment'
    public static final String IMPORT_ACTION = 'import'
    public static final String EDIT_DESCRIPTION_ACTION = 'editDescription'
    public static final String SOFT_DELETE_ACTION = 'softDelete'
    public static final String NEW_DOCUMENTATION_ACTION = 'newDocumentationVersion'
    public static final String NEW_BRANCH_MODEL_VERSION_ACTION = 'newBranchModelVersion'
    public static final String FINALISE_ACTION = 'finalise'
    public static final String CREATE_NEW_VERSIONS_ACTION = 'createNewVersions'
    public static final String NEW_FORK_MODEL_ACTION = 'newForkModel'
    public static final String NEW_MODEL_VERSION_ACTION = 'newModelVersion'
    public static final String MERGE_INTO_ACTION = 'mergeInto'
    public static final String READ_BY_EVERYONE_ACTION = 'readByEveryone'
    public static final String READ_BY_AUTHENTICATED_ACTION = 'readByAuthenticated'
    public static final String CAN_ADD_RULE_ACTION = 'canAddRule'

    public static final List<String> READER_VERSIONING_ACTIONS = [CREATE_NEW_VERSIONS_ACTION,
                                                                  NEW_FORK_MODEL_ACTION]

    public static final List<String> EDITOR_VERSIONING_ACTIONS = READER_VERSIONING_ACTIONS +
                                                                 [NEW_MODEL_VERSION_ACTION,
                                                                  NEW_DOCUMENTATION_ACTION,
                                                                  NEW_BRANCH_MODEL_VERSION_ACTION]

    public static final List<String> READ_ONLY_ACTIONS = [SHOW_ACTION]

    public static final List<String> STANDARD_EDIT_ACTIONS = READ_ONLY_ACTIONS +
                                                             [UPDATE_ACTION,
                                                              DELETE_ACTION]

    public static final List<String> STANDARD_CREATE_AND_EDIT_ACTIONS = STANDARD_EDIT_ACTIONS +
                                                                        [SAVE_ACTION]

    public static final List<String> SOFT_CREATE_AND_EDIT_ACTIONS = [UPDATE_ACTION,
                                                                     SOFT_DELETE_ACTION,
                                                                     SAVE_ACTION]

    public static final List<String> FULL_DELETE_ACTIONS = [SOFT_DELETE_ACTION,
                                                            DELETE_ACTION]

    public static final List<String> READER_ACTIONS = READ_ONLY_ACTIONS

    public static final List<String> REVIEWER_ACTIONS = READER_ACTIONS +
                                                        [COMMENT_ACTION]

    public static final List<String> AUTHOR_ACTIONS = REVIEWER_ACTIONS +
                                                      [EDIT_DESCRIPTION_ACTION]

    public static final List<String> EDITOR_ACTIONS = AUTHOR_ACTIONS +
                                                      SOFT_CREATE_AND_EDIT_ACTIONS +
                                                      [CAN_ADD_RULE_ACTION]

    public static final List<String> CONTAINER_ADMIN_ACTIONS = EDITOR_ACTIONS +
                                                               [DELETE_ACTION]

    public static final List<String> DISALLOWED_ONCE_FINALISED_ACTIONS = [UPDATE_ACTION,
                                                                          SAVE_ACTION,
                                                                          EDIT_DESCRIPTION_ACTION,
                                                                          FINALISE_ACTION]

    public static final List<String> DISALLOWED_MODELITEM_ACTIONS = [SOFT_DELETE_ACTION,
                                                                     FINALISE_ACTION]

    public static final List<String> USER_ADMIN_ACTIONS = [SHOW_ACTION,
                                                           UPDATE_ACTION,
                                                           DISABLE_ACTION]
}
